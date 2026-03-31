package com.english.learn.service;

import com.english.learn.constant.QuizQuestionType;
import com.english.learn.constant.QuizSessionStatus;
import com.english.learn.dto.quiz.*;
import com.english.learn.entity.*;
import com.english.learn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    private static final String COMBINED_SEP = "\n<__ANS_SPLIT__>\n";

    private final CardRepository cardRepository;
    private final CardSenseRepository cardSenseRepository;
    private final CardExampleRepository cardExampleRepository;
    private final CardSynonymRepository cardSynonymRepository;
    private final CardProgressRepository cardProgressRepository;
    private final AiNoteService aiNoteService;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionItemRepository quizSessionItemRepository;

    @Transactional(rollbackFor = Exception.class)
    public QuizSessionStartResponse startSession(Long userId, QuizSessionStartRequest req) {
        long docId = req.getDocumentId();
        int wantCards = req.getQuestionCount() == null ? 10 : req.getQuestionCount();
        wantCards = Math.max(1, Math.min(wantCards, 50));
        boolean prioritizeWrong = req.getPrioritizeWrong() == null || Boolean.TRUE.equals(req.getPrioritizeWrong());
        boolean prioritizeLow = req.getPrioritizeLowProficiency() == null || Boolean.TRUE.equals(req.getPrioritizeLowProficiency());
        boolean useAiTempExamples = Boolean.TRUE.equals(req.getUseAiTempExamples())
                && StringUtils.hasText(req.getAiApiKey());

        List<Card> cards = cardRepository.findByUserIdAndDocumentId(userId, docId);
        if (cards.isEmpty()) throw new IllegalArgumentException("该文档下没有卡片");
        Map<Long, Card> cardMap = cards.stream().collect(Collectors.toMap(Card::getId, c -> c));
        List<Long> cardIds = new ArrayList<>(cardMap.keySet());

        List<CardSense> senses = cardSenseRepository.findByCardIdIn(cardIds);
        List<Long> senseIds = senses.stream().map(CardSense::getId).collect(Collectors.toList());
        if (senseIds.isEmpty()) throw new IllegalArgumentException("没有义项数据，请先在卡片中生成或编辑结构化释义");
        Map<Long, List<CardSense>> sensesByCard = new HashMap<>();
        for (CardSense s : senses) sensesByCard.computeIfAbsent(s.getCardId(), k -> new ArrayList<>()).add(s);

        List<CardExample> allExamples = cardExampleRepository.findBySenseIdIn(senseIds);
        Map<Long, Long> senseToCard = senses.stream().collect(Collectors.toMap(CardSense::getId, CardSense::getCardId, (a, b) -> a));
        Map<Long, List<CardExample>> examplesByCard = new HashMap<>();
        for (CardExample ex : allExamples) {
            Long cid = senseToCard.get(ex.getSenseId());
            if (cid == null) continue;
            examplesByCard.computeIfAbsent(cid, k -> new ArrayList<>()).add(ex);
        }
        List<Long> cardsWithExamples = cardIds.stream().filter(examplesByCard::containsKey).collect(Collectors.toList());
        if (cardsWithExamples.isEmpty()) throw new IllegalArgumentException("没有可用例句，请先为例句录入或生成结构化内容");

        List<CardSynonym> allSynonyms = cardSynonymRepository.findBySenseIdIn(senseIds);
        Map<Long, List<CardSynonym>> synonymsByCard = new HashMap<>();
        for (CardSynonym syn : allSynonyms) {
            Long cid = senseToCard.get(syn.getSenseId());
            if (cid == null || !StringUtils.hasText(syn.getLemma())) continue;
            synonymsByCard.computeIfAbsent(cid, k -> new ArrayList<>()).add(syn);
        }
        List<Long> orderedCards = rankCardsByPriority(userId, cardsWithExamples, prioritizeWrong, prioritizeLow);

        QuizSession session = new QuizSession();
        session.setUserId(userId);
        session.setDocumentId(docId);
        session.setTotalCount(wantCards);
        session.setStatus(QuizSessionStatus.IN_PROGRESS);
        session = quizSessionRepository.save(session);

        QuizSessionStartResponse resp = new QuizSessionStartResponse();
        resp.setSessionId(session.getId());

        // 每张卡生成一个“组合题”：上半词条、下半中译英。
        List<QuestionPlan> pairedPlans = new ArrayList<>();
        for (Long cardId : orderedCards) {
            if (pairedPlans.size() >= wantCards) break;
            Card card = cardMap.get(cardId);
            if (card == null) continue;
            CardExample exB = pickExampleWithZh(examplesByCard.get(cardId));
            CardSense defSense = pickDefinitionSense(sensesByCard.get(cardId));
            if (defSense == null || exB == null
                    || !StringUtils.hasText(exB.getSentenceZh())
                    || !StringUtils.hasText(exB.getSentenceEn())) {
                continue;
            }
            String zhPrompt = exB.getSentenceZh().trim();
            String expectedEn = exB.getSentenceEn().trim();
            if (useAiTempExamples) {
                String[] aiPair = generateTempExampleWithAi(
                        card.getFrontContent(),
                        defSenseText(sensesByCard.get(cardId)),
                        req.getAiApiKey(), req.getAiModel(), req.getAiBaseUrl());
                if (StringUtils.hasText(aiPair[0]) && StringUtils.hasText(aiPair[1])) {
                    zhPrompt = aiPair[1];
                    expectedEn = aiPair[0];
                }
            }

            pairedPlans.add(QuestionPlan.combined(
                    cardId,
                    exB.getId(),
                    card.getFrontContent(),
                    buildDefinitionPrompt(defSense, card.getFrontContent(), synonymsByCard.get(cardId)),
                    zhPrompt,
                    expectedEn));
        }
        if (pairedPlans.isEmpty()) {
            throw new IllegalArgumentException("没有可用双阶段题目：请先确保卡片具备释义与中英例句");
        }
        int seq = 0;
        for (QuestionPlan p : pairedPlans) {
            seq++;
            createItemAndQuestion(resp, session.getId(), seq, p.cardId, p.exampleId, p.type, p.prompt, p.options, p.expected, p.sentenceEn, p.sentenceZh);
        }

        session.setTotalCount(resp.getQuestions().size());
        quizSessionRepository.save(session);
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public QuizAnswerResponse submitAnswer(Long userId, Long sessionId, QuizAnswerRequest req) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        if (QuizSessionStatus.COMPLETED.equals(session.getStatus())) {
            throw new IllegalArgumentException("本会话已结束");
        }
        QuizSessionItem item = quizSessionItemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));
        if (!item.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("题目不属于该会话");
        }
        Card card = cardRepository.findById(item.getCardId()).orElse(null);
        if (card != null && !card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        CardExample ex = cardExampleRepository.findById(item.getExampleId()).orElse(null);

        String expected = item.getExpectedText();
        String[] expectedPair = splitCombinedExpected(expected);
        String expectedFront = expectedPair[0];
        String expectedSentence = expectedPair[1];
        JudgeOutcome outcome;
        boolean frontOkForCombined = false;
        JudgeOutcome sentenceOutcomeForCombined = null;
        if (QuizQuestionType.COMBINED_INPUT.equals(item.getQuestionType())) {
            boolean frontOk = normalize(req.getAnswer()).equals(normalize(expectedFront));
            JudgeOutcome sentenceOutcome = judgeSentenceAnswer(
                    req.getAnswerExtra(), expectedSentence, card != null ? card.getFrontContent() : null, req);
            // 组合题总判定：词条与句子分开评分，避免句子必须逐字相同才判错/对
            int totalScore = (frontOk ? 40 : 0) + (int) Math.round(sentenceOutcome.score * 0.6);
            boolean ok = frontOk && ("CORRECT".equals(sentenceOutcome.verdict) || sentenceOutcome.score >= 85);
            String verdict = ok ? "CORRECT" : (totalScore >= 60 ? "PARTIAL" : "WRONG");
            String fb = "词条" + (frontOk ? "正确" : "不匹配")
                    + "；句子：" + (sentenceOutcome.feedback == null ? "-" : sentenceOutcome.feedback);
            outcome = new JudgeOutcome(ok, totalScore, verdict, fb);
            frontOkForCombined = frontOk;
            sentenceOutcomeForCombined = sentenceOutcome;
        } else if (QuizQuestionType.SENTENCE_TRANSLATION.equals(item.getQuestionType())) {
            outcome = judgeSentenceAnswer(req.getAnswer(), expected, card != null ? card.getFrontContent() : null, req);
        } else {
            boolean c = normalize(req.getAnswer()).equals(normalize(expected));
            outcome = new JudgeOutcome(c, c ? 100 : 0, c ? "CORRECT" : "WRONG", c ? "回答正确。" : "词条不匹配。");
        }
        if (QuizQuestionType.COMBINED_INPUT.equals(item.getQuestionType())) {
            item.setUserAnswer(safeTrim(req.getAnswer()) + COMBINED_SEP + safeTrim(req.getAnswerExtra()));
        } else {
            item.setUserAnswer(req.getAnswer());
        }
        item.setIsCorrect(outcome.correct);
        item.setAnsweredAt(LocalDateTime.now());
        quizSessionItemRepository.save(item);

        QuizAnswerResponse out = new QuizAnswerResponse();
        out.setCorrect(outcome.correct);
        out.setVerdict(outcome.verdict);
        out.setScore(outcome.score);
        out.setFeedback(outcome.feedback);
        out.setExpectedFront(expectedFront);
        out.setExpectedSentence(expectedSentence);
        out.setSentenceEn(ex != null ? ex.getSentenceEn() : null);
        out.setSentenceZh(ex != null ? ex.getSentenceZh() : null);
        if (QuizQuestionType.COMBINED_INPUT.equals(item.getQuestionType())) {
            out.setFrontCorrect(frontOkForCombined);
            out.setSentenceVerdict(sentenceOutcomeForCombined == null ? null : sentenceOutcomeForCombined.verdict);
            out.setSentenceScore(sentenceOutcomeForCombined == null ? null : sentenceOutcomeForCombined.score);
            out.setFrontFeedback(frontOkForCombined ? "词条正确。" : "词条与标准答案不一致。");
            out.setSentenceFeedback(sentenceOutcomeForCombined == null ? null : sentenceOutcomeForCombined.feedback);
        }

        List<QuizSessionItem> items = quizSessionItemRepository.findBySessionIdOrderBySequenceNoAsc(sessionId);
        boolean allDone = items.stream().allMatch(i -> i.getIsCorrect() != null);
        out.setSessionCompleted(allDone);
        if (allDone) {
            session.setStatus(QuizSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            quizSessionRepository.save(session);
        }
        return out;
    }

    public QuizResultResponse getResult(Long userId, Long sessionId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        List<QuizSessionItem> items = quizSessionItemRepository.findBySessionIdOrderBySequenceNoAsc(sessionId);
        QuizResultResponse r = new QuizResultResponse();
        r.setSessionId(sessionId);
        r.setTotal(items.size());
        int ok = 0;
        for (QuizSessionItem it : items) {
            QuizResultItemDTO row = new QuizResultItemDTO();
            row.setItemId(it.getId());
            row.setSequence(it.getSequenceNo());
            row.setIsCorrect(it.getIsCorrect());
            row.setUserAnswer(it.getUserAnswer());
            row.setType(it.getQuestionType());
            row.setPrompt(it.getPromptText());
            if (QuizQuestionType.COMBINED_INPUT.equals(it.getQuestionType())) {
                String[] pair = splitCombinedExpected(it.getExpectedText());
                String[] userPair = splitCombinedExpected(it.getUserAnswer());
                row.setExpected("词条: " + safeTrim(pair[0]) + " | 句子: " + safeTrim(pair[1]));
                row.setUserAnswer("词条: " + safeTrim(userPair[0]) + " | 句子: " + safeTrim(userPair[1]));
            } else {
                row.setExpected(it.getExpectedText());
            }
            CardExample ex = cardExampleRepository.findById(it.getExampleId()).orElse(null);
            row.setSentenceEn(ex != null ? ex.getSentenceEn() : null);
            r.getItems().add(row);
            if (Boolean.TRUE.equals(it.getIsCorrect())) ok++;
        }
        r.setCorrectCount(ok);
        return r;
    }

    public QuizSessionStartResponse getSessionDetail(Long userId, Long sessionId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        List<QuizSessionItem> items = quizSessionItemRepository.findBySessionIdOrderBySequenceNoAsc(sessionId);
        QuizSessionStartResponse resp = new QuizSessionStartResponse();
        resp.setSessionId(sessionId);
        for (QuizSessionItem it : items) {
            QuizQuestionDTO q = new QuizQuestionDTO();
            q.setItemId(it.getId());
            q.setSequence(it.getSequenceNo());
            q.setType(it.getQuestionType());
            q.setPrompt(it.getPromptText());
            q.setOptions(parseOptions(it.getOptionsJson()));
            CardExample ex = cardExampleRepository.findById(it.getExampleId()).orElse(null);
            applyQuestionDisplayFields(q, it, ex);
            resp.getQuestions().add(q);
        }
        return resp;
    }

    public List<QuizResultResponse> listRecentResults(Long userId) {
        List<QuizSession> sessions = quizSessionRepository.findTop30ByUserIdOrderByGmtCreateDesc(userId);
        List<QuizResultResponse> out = new ArrayList<>();
        for (QuizSession s : sessions) {
            // 复用 getResult 的格式；前端可直接展示
            try {
                out.add(getResult(userId, s.getId()));
            } catch (Exception ignore) {
                // ignore
            }
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public QuizSessionStartResponse retryWrong(Long userId, Long sessionId) {
        QuizSession old = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        if (!old.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        List<QuizSessionItem> oldItems = quizSessionItemRepository.findBySessionIdOrderBySequenceNoAsc(sessionId);
        List<QuizSessionItem> wrongItems = oldItems.stream()
                .filter(i -> Boolean.FALSE.equals(i.getIsCorrect()))
                .collect(Collectors.toList());
        if (wrongItems.isEmpty()) {
            throw new IllegalArgumentException("没有错题");
        }

        QuizSession session = new QuizSession();
        session.setUserId(userId);
        session.setDocumentId(old.getDocumentId());
        session.setTotalCount(wrongItems.size());
        session.setStatus(QuizSessionStatus.IN_PROGRESS);
        session = quizSessionRepository.save(session);

        QuizSessionStartResponse resp = new QuizSessionStartResponse();
        resp.setSessionId(session.getId());
        Collections.shuffle(wrongItems, ThreadLocalRandom.current());
        int seq = 0;
        for (QuizSessionItem oi : wrongItems) {
            seq++;
            QuizSessionItem item = new QuizSessionItem();
            item.setSessionId(session.getId());
            item.setSequenceNo(seq);
            item.setCardId(oi.getCardId());
            item.setExampleId(oi.getExampleId());
            item.setQuestionType(oi.getQuestionType());
            item.setPromptText(oi.getPromptText());
            item.setOptionsJson(oi.getOptionsJson());
            item.setExpectedText(oi.getExpectedText());
            item = quizSessionItemRepository.save(item);
            QuizQuestionDTO q = new QuizQuestionDTO();
            q.setItemId(item.getId());
            q.setSequence(seq);
            q.setType(item.getQuestionType());
            q.setPrompt(item.getPromptText());
            q.setOptions(parseOptions(item.getOptionsJson()));
            CardExample ex = cardExampleRepository.findById(oi.getExampleId()).orElse(null);
            applyQuestionDisplayFields(q, item, ex);
            resp.getQuestions().add(q);
        }
        return resp;
    }

    private void applyQuestionDisplayFields(QuizQuestionDTO q, QuizSessionItem it, CardExample ex) {
        String type = it.getQuestionType();
        if (QuizQuestionType.COMBINED_INPUT.equals(type)) {
            q.setSentenceEn(null);
            q.setSentenceZh(null);
            return;
        }
        if (ex == null) return;
        if (QuizQuestionType.FRONT_INPUT.equals(type)) {
            Card card = cardRepository.findById(it.getCardId()).orElse(null);
            String front = card != null ? card.getFrontContent() : null;
            String[] clues = frontInputClues(ex, front);
            q.setSentenceEn(clues[0]);
            q.setSentenceZh(clues[1]);
            return;
        }
        if (QuizQuestionType.SENTENCE_TRANSLATION.equals(type)) {
            q.setSentenceEn(null);
            q.setSentenceZh(null);
            return;
        }
        if (QuizQuestionType.SYNONYM_CHOICE.equals(type)
                || QuizQuestionType.DEFINITION_INPUT.equals(type)) {
            q.setSentenceEn(null);
            q.setSentenceZh(null);
            return;
        }
        q.setSentenceEn(ex.getSentenceEn());
        q.setSentenceZh(ex.getSentenceZh());
    }

    /**
     * FRONT_INPUT：尽量不泄露整条英文原句；优先在句中用下划线遮住词条，遮不住则只给中文提示。
     */
    private static String[] frontInputClues(CardExample ex, String frontContent) {
        if (ex == null) return new String[]{null, null};
        String en = ex.getSentenceEn();
        String zh = ex.getSentenceZh();
        if (StringUtils.hasText(en)) {
            en = en.replaceFirst("(?i)^\\s*example\\(s\\)\\s*[-:：]?\\s*", "").trim();
        }
        String masked = maskTargetInText(en, frontContent);
        if (StringUtils.hasText(masked) && !masked.equals(en)) {
            return new String[]{masked, StringUtils.hasText(zh) ? zh.trim() : null};
        }
        if (StringUtils.hasText(zh)) {
            return new String[]{null, zh.trim()};
        }
        return new String[]{en, null};
    }

    private static String maskTargetInText(String sentence, String target) {
        if (!StringUtils.hasText(sentence) || !StringUtils.hasText(target)) return sentence;
        String t = target.trim();
        Pattern p = Pattern.compile(Pattern.quote(t), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(sentence);
        if (!m.find()) return sentence;
        return m.replaceAll("______");
    }

    /** 释义里常以 " Lemma " / 「Lemma」 等形式直接写出目标词，需一并遮盖。 */
    private static String maskQuotedLemma(String text, String target) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(target)) return text;
        String t = Pattern.quote(target.trim());
        Pattern p = Pattern.compile(
                "(?i)([\"'`\u201c\u201d\u2018\u2019]|「)\\s*" + t + "\\s*([\"'`\u201c\u201d\u2018\u2019]|」)");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "______" + m.group(2)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static CardExample pickExample(List<CardExample> list) {
        if (list == null || list.isEmpty()) return null;
        List<CardExample> copy = new ArrayList<>(list);
        Collections.shuffle(copy, ThreadLocalRandom.current());
        return copy.get(0);
    }

    private static CardExample pickExampleWithZh(List<CardExample> list) {
        if (list == null || list.isEmpty()) return null;
        List<CardExample> withZh = list.stream()
                .filter(e -> StringUtils.hasText(e.getSentenceZh()) && StringUtils.hasText(e.getSentenceEn()))
                .collect(Collectors.toList());
        if (withZh.isEmpty()) return null;
        Collections.shuffle(withZh, ThreadLocalRandom.current());
        return withZh.get(0);
    }

    private void createItemAndQuestion(
            QuizSessionStartResponse resp,
            Long sessionId,
            int seq,
            Long cardId,
            Long exampleId,
            String type,
            String prompt,
            List<String> options,
            String expected,
            String sentenceEn,
            String sentenceZh
    ) {
        QuizSessionItem item = new QuizSessionItem();
        item.setSessionId(sessionId);
        item.setSequenceNo(seq);
        item.setCardId(cardId);
        item.setExampleId(exampleId);
        item.setQuestionType(type);
        item.setPromptText(prompt);
        item.setOptionsJson(options == null ? null : toJsonArray(options));
        item.setExpectedText(expected);
        item = quizSessionItemRepository.save(item);

        QuizQuestionDTO q = new QuizQuestionDTO();
        q.setItemId(item.getId());
        q.setSequence(seq);
        q.setType(type);
        q.setPrompt(prompt);
        q.setOptions(options);
        q.setSentenceEn(sentenceEn);
        q.setSentenceZh(sentenceZh);
        resp.getQuestions().add(q);
    }

    private static List<String> buildOptions(String correct, List<String> pool, int size) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (StringUtils.hasText(correct)) set.add(correct.trim());
        if (pool != null && !pool.isEmpty()) {
            List<String> copy = new ArrayList<>(pool);
            Collections.shuffle(copy, ThreadLocalRandom.current());
            for (String s : copy) {
                if (!StringUtils.hasText(s)) continue;
                set.add(s.trim());
                if (set.size() >= size) break;
            }
        }
        List<String> out = new ArrayList<>(set);
        Collections.shuffle(out, ThreadLocalRandom.current());
        return out;
    }

    private static CardSense pickDefinitionSense(List<CardSense> senses) {
        if (senses == null || senses.isEmpty()) return null;
        List<CardSense> cands = senses.stream()
                .filter(s -> StringUtils.hasText(s.getExplanationEn()) || StringUtils.hasText(s.getTranslationZh()))
                .collect(Collectors.toList());
        if (cands.isEmpty()) return null;
        Collections.shuffle(cands, ThreadLocalRandom.current());
        return cands.get(0);
    }

    private static String buildDefinitionPrompt(CardSense s, String front, List<CardSynonym> synonyms) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据下面的释义与同义词线索，填写对应的英文词条（卡片正面）：\n");
        if (StringUtils.hasText(s.getExplanationEn())) {
            String def = s.getExplanationEn().trim();
            def = def.replaceFirst("(?i)^\\s*example\\(s\\)\\s*[-:：]?\\s*", "");
            def = maskTargetInText(def, front);
            def = maskQuotedLemma(def, front);
            sb.append("EN Definition: ").append(def).append("\n");
        }
        if (StringUtils.hasText(s.getTranslationZh())) {
            sb.append("ZH 释义提示: ").append(s.getTranslationZh().trim()).append("\n");
        }
        List<String> synHints = synonymHints(synonyms, 3, front);
        if (!synHints.isEmpty()) {
            sb.append("同义词线索: ").append(String.join(", ", synHints)).append("\n");
        }
        sb.append("请写词条，不要写整句。");
        return sb.toString().trim();
    }

    private static List<String> synonymHints(List<CardSynonym> synonyms, int max, String front) {
        if (synonyms == null || synonyms.isEmpty()) return Collections.emptyList();
        String f = safeTrim(front).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (CardSynonym s : synonyms) {
            if (s == null || !StringUtils.hasText(s.getLemma())) continue;
            String lemma = s.getLemma().trim();
            if (lemma.equalsIgnoreCase(f)) continue;
            String hint = lemma;
            if (StringUtils.hasText(s.getNoteZh())) {
                hint = hint + "（" + s.getNoteZh().trim() + "）";
            }
            out.add(hint);
            if (out.size() >= max) break;
        }
        return out;
    }

    private static String defSenseText(List<CardSense> senses) {
        if (senses == null || senses.isEmpty()) return "";
        CardSense s = senses.get(0);
        String zh = s.getTranslationZh() == null ? "" : s.getTranslationZh().trim();
        String en = s.getExplanationEn() == null ? "" : s.getExplanationEn().trim();
        if (!zh.isEmpty() && !en.isEmpty()) return zh + " | " + en;
        return zh + en;
    }

    private String[] generateTempExampleWithAi(String front, String senseHint, String apiKey, String model, String baseUrl) {
        String prompt = "Generate ONE bilingual example sentence for vocabulary test.\n"
                + "Target word/phrase: " + safeTrim(front) + "\n"
                + "Sense hint: " + safeTrim(senseHint) + "\n"
                + "Return ONLY one JSON object: {\"en\":\"...\",\"zh\":\"...\"}\n"
                + "Requirements: en must naturally use the target; zh must be natural Chinese.";
        Optional<String> out = aiNoteService.chatWithConfig(prompt, apiKey, model, baseUrl);
        if (!out.isPresent()) return new String[]{null, null};
        String t = out.get();
        String en = matchJsonField(t, "en");
        String zh = matchJsonField(t, "zh");
        return new String[]{trimToNull(en), trimToNull(zh)};
    }

    private JudgeOutcome judgeSentenceAnswer(String user, String expected, String target, QuizAnswerRequest req) {
        String u = user == null ? "" : user.trim();
        String e = expected == null ? "" : expected.trim();
        if (u.isEmpty() || e.isEmpty()) return new JudgeOutcome(false, 0, "WRONG", "答案为空或参考句缺失。");

        // 规则兜底：包含目标词 + 与参考句词汇重叠达到阈值
        int fallbackScore = sentenceRuleScore(u, e, target);
        JudgeOutcome fallback = fromScore(fallbackScore);

        if (!StringUtils.hasText(req.getAiApiKey())) return fallback;
        String prompt = "You are grading an English translation answer for meaning equivalence.\n"
                + "Target term: " + safeTrim(target) + "\n"
                + "Reference sentence: " + e + "\n"
                + "User answer: " + u + "\n"
                + "Return ONLY JSON: {\"verdict\":\"CORRECT|PARTIAL|WRONG\",\"score\":0-100,\"feedback\":\"short feedback\"}\n"
                + "Criteria:\n"
                + "1) Semantic equivalence is primary; do NOT require exact same wording.\n"
                + "2) Minor grammar/wording differences are acceptable if meaning is preserved.\n"
                + "3) Typos should be tolerated when understandable.\n"
                + "4) Prefer using target term, but close inflections/near forms are acceptable.\n"
                + "5) Only mark WRONG when meaning clearly deviates or misses key information.";
        Optional<String> out = aiNoteService.chatWithConfig(prompt, req.getAiApiKey(), req.getAiModel(), req.getAiBaseUrl());
        if (!out.isPresent()) return fallback;
        String verdict = trimToNull(matchJsonField(out.get(), "verdict"));
        String score = trimToNull(matchJsonField(out.get(), "score"));
        String feedback = trimToNull(matchJsonField(out.get(), "feedback"));
        if (verdict == null && score == null) return fallback;
        int sc = parseInt(score, fallback.score);
        JudgeOutcome aiOut = fromScore(sc);
        if (verdict != null) {
            String v = verdict.toUpperCase(Locale.ROOT);
            if ("CORRECT".equals(v) || "PARTIAL".equals(v) || "WRONG".equals(v)) {
                aiOut.verdict = v;
                aiOut.correct = "CORRECT".equals(v);
            }
        }
        if (feedback != null) aiOut.feedback = feedback;
        return aiOut;
    }

    private static int sentenceRuleScore(String user, String expected, String target) {
        String u = user.toLowerCase(Locale.ROOT);
        String e = expected.toLowerCase(Locale.ROOT);
        boolean targetMatched = true;
        if (StringUtils.hasText(target)) {
            targetMatched = containsTargetOrNearForm(u, target.toLowerCase(Locale.ROOT));
        }
        Set<String> us = Arrays.stream(u.replaceAll("[^a-z\\s]", " ").split("\\s+"))
                .filter(s -> s.length() > 2).collect(Collectors.toSet());
        Set<String> es = Arrays.stream(e.replaceAll("[^a-z\\s]", " ").split("\\s+"))
                .filter(s -> s.length() > 2).collect(Collectors.toSet());
        if (es.isEmpty()) return 0;
        int hit = 0;
        for (String w : es) if (us.contains(w)) hit++;
        double ratio = (double) hit / (double) es.size();
        int base = (int) Math.round(Math.min(100.0, Math.max(0.0, ratio * 100.0)));
        if (!targetMatched) base -= 20;
        return Math.max(0, Math.min(100, base));
    }

    private static boolean containsTargetOrNearForm(String userLower, String targetLower) {
        if (!StringUtils.hasText(userLower) || !StringUtils.hasText(targetLower)) return false;
        if (userLower.contains(targetLower)) return true;
        List<String> words = Arrays.stream(userLower.replaceAll("[^a-z\\s]", " ").split("\\s+"))
                .filter(s -> s.length() > 1).collect(Collectors.toList());
        String t = targetLower.trim();
        for (String w : words) {
            if (editDistanceLeqOne(w, t)) return true;
            if (w.startsWith(t) || t.startsWith(w)) return true;
        }
        return false;
    }

    private static boolean editDistanceLeqOne(String a, String b) {
        if (a == null || b == null) return false;
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        int i = 0, j = 0, diff = 0;
        while (i < la && j < lb) {
            if (a.charAt(i) == b.charAt(j)) {
                i++;
                j++;
            } else {
                diff++;
                if (diff > 1) return false;
                if (la > lb) i++;
                else if (lb > la) j++;
                else {
                    i++;
                    j++;
                }
            }
        }
        if (i < la || j < lb) diff++;
        return diff <= 1;
    }

    private static JudgeOutcome fromScore(int score) {
        int s = Math.max(0, Math.min(100, score));
        if (s >= 80) return new JudgeOutcome(true, s, "CORRECT", "语义与表达较好。");
        if (s >= 50) return new JudgeOutcome(false, s, "PARTIAL", "部分正确，建议优化措辞或语法。");
        return new JudgeOutcome(false, s, "WRONG", "与目标句语义差距较大。");
    }

    private static int parseInt(String raw, int dft) {
        if (raw == null) return dft;
        try { return Integer.parseInt(raw.trim()); } catch (Exception ignore) { return dft; }
    }

    private static String matchJsonField(String json, String field) {
        if (json == null) return null;
        Matcher m = Pattern.compile("(?is)\"" + Pattern.quote(field) + "\"\\s*:\\s*(\"([^\"]*)\"|(true|false|\\d+))").matcher(json);
        if (!m.find()) return null;
        String g2 = m.group(2);
        if (g2 != null) return g2;
        String raw = m.group(1);
        if (raw == null) return null;
        return raw.replace("\"", "");
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static class JudgeOutcome {
        private boolean correct;
        private int score;
        private String verdict;
        private String feedback;

        private JudgeOutcome(boolean correct, int score, String verdict, String feedback) {
            this.correct = correct;
            this.score = score;
            this.verdict = verdict;
            this.feedback = feedback;
        }
    }

    private List<Long> rankCardsByPriority(Long userId, List<Long> cardIds, boolean prioritizeWrong, boolean prioritizeLow) {
        List<Long> out = new ArrayList<>(cardIds);
        Map<Long, CardProgress> progressMap = new HashMap<>();
        List<CardProgress> progresses = cardProgressRepository.findByUserIdAndCardIdIn(userId, cardIds);
        for (CardProgress p : progresses) progressMap.put(p.getCardId(), p);
        Collections.shuffle(out, ThreadLocalRandom.current());
        out.sort((a, b) -> Double.compare(score(progressMap.get(b), prioritizeWrong, prioritizeLow),
                score(progressMap.get(a), prioritizeWrong, prioritizeLow)));
        return out;
    }

    private static double score(CardProgress p, boolean prioritizeWrong, boolean prioritizeLow) {
        if (p == null) return 1.0;
        double s = 0.0;
        Integer prof = p.getProficiencyLevel();
        if (prioritizeWrong && (prof == null || prof <= 2)) s += 10.0;
        if (prioritizeLow) s += Math.max(0, 6 - (prof == null ? 1 : prof)) * 2.0;
        Integer rc = p.getReviewCount();
        if (rc != null) s += Math.min(2.5, rc * 0.2);
        return s;
    }

    private static class QuestionPlan {
        private Long cardId;
        private Long exampleId;
        private String type;
        private String prompt;
        private List<String> options;
        private String expected;
        private String sentenceEn;
        private String sentenceZh;

        static QuestionPlan combined(Long cardId, Long exampleId, String expectedFront, String definitionPrompt, String zhPrompt, String expectedEn) {
            QuestionPlan p = new QuestionPlan();
            p.cardId = cardId;
            p.exampleId = exampleId;
            p.type = QuizQuestionType.COMBINED_INPUT;
            p.prompt = definitionPrompt
                    + "\n\n下半题：请把下面中文翻译成英文句子（尽量使用本卡词条）：\n"
                    + safeTrim(zhPrompt);
            p.expected = safeTrim(expectedFront) + COMBINED_SEP + safeTrim(expectedEn);
            p.sentenceZh = null;
            return p;
        }

        static QuestionPlan frontInput(Long cardId, Long exampleId, String expected, String sentenceEn, String sentenceZh) {
            QuestionPlan p = new QuestionPlan();
            p.cardId = cardId;
            p.exampleId = exampleId;
            p.type = QuizQuestionType.FRONT_INPUT;
            p.prompt = "请根据下面的提示，在输入框中填写「卡片正面」的英文词条（通常是单词或短语，不要抄写整句）。";
            p.expected = expected;
            p.sentenceEn = sentenceEn;
            p.sentenceZh = sentenceZh;
            return p;
        }

        static QuestionPlan definition(Long cardId, Long exampleId, String expected, String prompt) {
            QuestionPlan p = new QuestionPlan();
            p.cardId = cardId;
            p.exampleId = exampleId;
            p.type = QuizQuestionType.DEFINITION_INPUT;
            p.prompt = prompt;
            p.expected = expected;
            return p;
        }

        static QuestionPlan synonymChoice(Long cardId, Long exampleId, String expected, List<String> options, String front) {
            QuestionPlan p = new QuestionPlan();
            p.cardId = cardId;
            p.exampleId = exampleId;
            p.type = QuizQuestionType.SYNONYM_CHOICE;
            p.prompt = "下列词条的一个同义 / 近义词是哪一个？（四项里只有一项正确）\n词条：「" + safeTrim(front) + "」";
            p.options = options;
            p.expected = expected;
            return p;
        }

        static QuestionPlan sentenceTranslation(Long cardId, Long exampleId, String front, String zhPrompt, String expectedEn) {
            QuestionPlan p = new QuestionPlan();
            p.cardId = cardId;
            p.exampleId = exampleId;
            p.type = QuizQuestionType.SENTENCE_TRANSLATION;
            p.prompt = "请把下面中文翻译成英文句子（尽量使用词条「" + safeTrim(front) + "」）：\n" + safeTrim(zhPrompt);
            p.expected = expectedEn;
            return p;
        }
    }

    private static String pickSynonymLemma(List<CardSynonym> list) {
        if (list == null || list.isEmpty()) return null;
        List<String> lemmas = list.stream().map(CardSynonym::getLemma).filter(StringUtils::hasText).collect(Collectors.toList());
        if (lemmas.isEmpty()) return null;
        Collections.shuffle(lemmas, ThreadLocalRandom.current());
        return lemmas.get(0).trim();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String[] splitCombinedExpected(String raw) {
        if (!StringUtils.hasText(raw)) return new String[]{null, null};
        int idx = raw.indexOf(COMBINED_SEP);
        if (idx < 0) return new String[]{raw, null};
        String a = raw.substring(0, idx);
        String b = raw.substring(idx + COMBINED_SEP.length());
        return new String[]{trimToNull(a), trimToNull(b)};
    }

    private static String toJsonArray(List<String> arr) {
        // minimal JSON builder to avoid adding deps
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String s : arr) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(s)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static List<String> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) return null;
        String t = optionsJson.trim();
        if (!t.startsWith("[") || !t.endsWith("]")) return null;
        t = t.substring(1, t.length() - 1).trim();
        if (t.isEmpty()) return new ArrayList<>();
        // very small parser for ["a","b"] only
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        boolean esc = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!inStr) {
                if (c == '"') {
                    inStr = true;
                    cur.setLength(0);
                }
                continue;
            }
            if (esc) {
                cur.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                inStr = false;
                out.add(cur.toString());
            } else {
                cur.append(c);
            }
        }
        return out;
    }
}
