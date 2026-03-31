package com.english.learn.service;

import com.english.learn.dto.CardExampleDTO;
import com.english.learn.dto.CardGlobalExtraDTO;
import com.english.learn.dto.CardSenseDTO;
import com.english.learn.dto.CardSynonymDTO;
import com.english.learn.dto.structured.*;
import com.english.learn.entity.*;
import com.english.learn.repository.*;
import com.english.learn.util.CardBackContentAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 义项 / 例句 / 同义词 / 全局扩展 的持久化与批量加载。
 */
@Service
@RequiredArgsConstructor
public class CardStructuredContentService {

    private final CardRepository cardRepository;
    private final CardSenseRepository cardSenseRepository;
    private final CardExampleRepository cardExampleRepository;
    private final CardSynonymRepository cardSynonymRepository;
    private final CardGlobalExtraRepository cardGlobalExtraRepository;
    private final AiStructuredCardService aiStructuredCardService;

    public void assertCardOwner(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作该卡片");
        }
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public void replaceStructure(Long cardId, Long userId, CardStructuredSaveRequest request) {
        assertCardOwner(cardId, userId);
        clearStructure(cardId);

        List<CardStructuredSensePayload> sensePayloads = request.getSenses() == null
                ? Collections.emptyList() : request.getSenses();
        if (sensePayloads.isEmpty()) {
            if (request.getGlobalExtra() != null && !isGlobalPayloadEmpty(request.getGlobalExtra())) {
                saveGlobalExtraRow(cardId, request.getGlobalExtra());
            } else {
                Card card = cardRepository.findById(cardId).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
                card.setBackContent("");
                cardRepository.save(card);
            }
            syncBackContent(cardId);
            return;
        }

        int senseOrder = 0;
        for (CardStructuredSensePayload sp : sensePayloads) {
            senseOrder++;
            int so = sp.getOrder() != null ? sp.getOrder() : senseOrder;
            CardSense sense = new CardSense();
            sense.setCardId(cardId);
            sense.setSortOrder(so);
            sense.setLabel(trimToNull(sp.getLabel()));
            sense.setTranslationZh(trimToNull(sp.getTranslationZh()));
            sense.setExplanationEn(trimToNull(sp.getExplanationEn()));
            sense.setTone(trimToNull(sp.getTone()));
            sense = cardSenseRepository.save(sense);
            Long senseId = sense.getId();

            int exOrder = 0;
            for (CardStructuredExamplePayload ep : nullSafe(sp.getExamples())) {
                if (ep.getEn() == null || ep.getEn().trim().isEmpty()) continue;
                exOrder++;
                CardExample ex = new CardExample();
                ex.setSenseId(senseId);
                ex.setSortOrder(ep.getOrder() != null ? ep.getOrder() : exOrder);
                ex.setSentenceEn(ep.getEn().trim());
                ex.setSentenceZh(trimToNull(ep.getZh()));
                ex.setScenarioTag(trimToNull(ep.getTag()));
                cardExampleRepository.save(ex);
            }

            int syOrder = 0;
            for (CardStructuredSynonymPayload yp : nullSafe(sp.getSynonyms())) {
                if (yp.getLemma() == null || yp.getLemma().trim().isEmpty()) continue;
                syOrder++;
                CardSynonym sy = new CardSynonym();
                sy.setSenseId(senseId);
                sy.setSortOrder(yp.getOrder() != null ? yp.getOrder() : syOrder);
                sy.setLemma(yp.getLemma().trim());
                sy.setNoteZh(trimToNull(yp.getNoteZh()));
                cardSynonymRepository.save(sy);
            }
        }

        if (request.getGlobalExtra() != null) {
            saveGlobalExtraRow(cardId, request.getGlobalExtra());
        }

        syncBackContent(cardId);
    }

    private void saveGlobalExtraRow(Long cardId, CardStructuredGlobalPayload gp) {
        CardGlobalExtra ge = new CardGlobalExtra();
        ge.setCardId(cardId);
        ge.setNativeTip(trimToNull(gp.getNativeTip()));
        ge.setHighLevelEn(trimToNull(gp.getHighLevelEn()));
        ge.setHighLevelZh(trimToNull(gp.getHighLevelZh()));
        if (gp.getCollocations() != null && !gp.getCollocations().isEmpty()) {
            try {
                ge.setCollocationsJson(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(gp.getCollocations()));
            } catch (Exception ignored) {
                ge.setCollocationsJson("[]");
            }
        }
        cardGlobalExtraRepository.save(ge);
    }

    private static boolean isGlobalPayloadEmpty(CardStructuredGlobalPayload gp) {
        if (gp == null) return true;
        boolean noCol = gp.getCollocations() == null || gp.getCollocations().isEmpty();
        boolean noTip = gp.getNativeTip() == null || gp.getNativeTip().trim().isEmpty();
        boolean noHl = (gp.getHighLevelEn() == null || gp.getHighLevelEn().trim().isEmpty())
                && (gp.getHighLevelZh() == null || gp.getHighLevelZh().trim().isEmpty());
        return noCol && noTip && noHl;
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public void applyAiJson(Long cardId, Long userId, String rawJson) {
        CardStructuredSaveRequest req = StructuredJsonMapper.parse(rawJson);
        replaceStructure(cardId, userId, req);
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public void generateAndApply(Long cardId, Long userId, CardStructuredGenerateRequest req) {
        assertCardOwner(cardId, userId);
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        String json = aiStructuredCardService.generateStructuredJson(
                card.getFrontContent(),
                req.getContextSentence(),
                req.getAiApiKey(),
                req.getAiModel(),
                req.getAiBaseUrl()
        ).orElseThrow(() -> new IllegalArgumentException("AI 未返回内容或调用失败"));
        applyAiJson(cardId, userId, json);
    }

    /**
     * 将 AI 生成的“背面注释文本”解析为结构化义项并落库。
     * 支持两类输入：
     * 1) 新块标签格式（[TRANSLATION_ZH]...）
     * 2) 旧格式（English definition / 中文释义 / Example(s)）
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public boolean tryApplyFromNoteText(Long cardId, Long userId, String noteText) {
        if (noteText == null || noteText.trim().isEmpty()) return false;
        Optional<CardStructuredSaveRequest> parsed = parseStructuredFromNoteText(noteText);
        if (!parsed.isPresent()) return false;
        replaceStructure(cardId, userId, parsed.get());
        return true;
    }

    public void clearStructure(Long cardId) {
        List<CardSense> senses = cardSenseRepository.findByCardIdOrderBySortOrderAsc(cardId);
        List<Long> senseIds = senses.stream().map(CardSense::getId).collect(Collectors.toList());
        if (!senseIds.isEmpty()) {
            cardExampleRepository.deleteBySenseIdIn(senseIds);
            cardSynonymRepository.deleteBySenseIdIn(senseIds);
        }
        cardSenseRepository.deleteByCardId(cardId);
        cardGlobalExtraRepository.deleteByCardId(cardId);
    }

    private Optional<CardStructuredSaveRequest> parseStructuredFromNoteText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return Optional.empty();

        // v2: 支持多义词多块 [SENSE]...[/SENSE]，每块内再按标签提取
        List<String> senseBlocks = extractRepeatedTagBlocks(text, "SENSE");

        String collocationsBlock = trimToNull(extractTagBlock(text, "COLLOCATIONS"));
        String nativeTip = trimToNull(extractTagBlock(text, "NATIVE_TIP"));
        String highLevelEn = trimToNull(extractTagBlock(text, "HIGH_LEVEL_EN"));
        String highLevelZh = trimToNull(extractTagBlock(text, "HIGH_LEVEL_ZH"));

        CardStructuredSaveRequest req = new CardStructuredSaveRequest();
        List<CardStructuredSensePayload> sensesOut = new ArrayList<>();

        if (senseBlocks != null && !senseBlocks.isEmpty()) {
            int order = 0;
            for (String block : senseBlocks) {
                order++;
                String zh = trimToNull(extractTagBlock(block, "TRANSLATION_ZH"));
                String en = trimToNull(extractTagBlock(block, "EXPLANATION_EN"));
                String examplesBlock = trimToNull(extractTagBlock(block, "EXAMPLES"));
                String synonymsBlock = trimToNull(extractTagBlock(block, "SYNONYMS"));

                List<CardStructuredExamplePayload> examples = parseExamples(examplesBlock);
                List<CardStructuredSynonymPayload> synonyms = parseSynonyms(synonymsBlock);
                boolean noCore = zh == null && en == null && examples.isEmpty() && synonyms.isEmpty();
                if (noCore) continue;

                CardStructuredSensePayload sense = new CardStructuredSensePayload();
                sense.setOrder(order);
                sense.setTranslationZh(zh);
                sense.setExplanationEn(en);
                sense.setExamples(examples);
                sense.setSynonyms(synonyms);
                sensesOut.add(sense);
            }
        }

        if (sensesOut.isEmpty()) {
            // 兼容旧格式（English definition / 中文释义 / Example(s)）+ 旧的单块标签格式
            String zh = trimToNull(extractTagBlock(text, "TRANSLATION_ZH"));
            String en = trimToNull(extractTagBlock(text, "EXPLANATION_EN"));
            String examplesBlock = trimToNull(extractTagBlock(text, "EXAMPLES"));
            String synonymsBlock = trimToNull(extractTagBlock(text, "SYNONYMS"));

            if (zh == null) zh = trimToNull(extractByKeywordLine(text, "中文释义"));
            if (en == null) en = trimToNull(extractByKeywordLine(text, "English definition"));
            if (examplesBlock == null) examplesBlock = trimToNull(extractSectionByHeader(text, "Example"));
            if (examplesBlock == null) examplesBlock = trimToNull(extractExamplesInline(text));
            if (synonymsBlock == null) synonymsBlock = trimToNull(extractSectionByHeader(text, "Synonym"));

            List<CardStructuredExamplePayload> examples = parseExamples(examplesBlock);
            if (examples.isEmpty()) {
                List<String> fallbackEn = findEnglishSentences(text, 3);
                int idx = 0;
                for (String s : fallbackEn) {
                    idx++;
                    CardStructuredExamplePayload ep = new CardStructuredExamplePayload();
                    ep.setOrder(idx);
                    ep.setEn(s);
                    examples.add(ep);
                }
            }
            List<CardStructuredSynonymPayload> synonyms = parseSynonyms(synonymsBlock);

            boolean noCore = zh == null && en == null && examples.isEmpty() && synonyms.isEmpty();
            if (noCore) return Optional.empty();

            CardStructuredSensePayload sense = new CardStructuredSensePayload();
            sense.setOrder(1);
            sense.setTranslationZh(zh);
            sense.setExplanationEn(en);
            sense.setExamples(examples);
            sense.setSynonyms(synonyms);
            sensesOut.add(sense);
        }

        req.setSenses(sensesOut);

        CardStructuredGlobalPayload gp = new CardStructuredGlobalPayload();
        if (collocationsBlock != null) {
            gp.setCollocations(parseCollocations(collocationsBlock));
        }
        gp.setNativeTip(nativeTip);
        gp.setHighLevelEn(highLevelEn);
        gp.setHighLevelZh(highLevelZh);
        if (!isGlobalPayloadEmpty(gp)) {
            req.setGlobalExtra(gp);
        }
        return Optional.of(req);
    }

    private static List<String> extractRepeatedTagBlocks(String text, String tag) {
        if (text == null) return Collections.emptyList();
        Pattern p = Pattern.compile("(?is)\\[" + Pattern.quote(tag) + "\\](.*?)\\[/" + Pattern.quote(tag) + "\\]");
        Matcher m = p.matcher(text);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            String b = m.group(1);
            if (b != null && !b.trim().isEmpty()) out.add(b.trim());
        }
        return out;
    }

    private static List<CardStructuredExamplePayload> parseExamples(String block) {
        List<CardStructuredExamplePayload> out = new ArrayList<>();
        if (block == null || block.trim().isEmpty()) return out;
        String[] lines = block.split("\\r?\\n");
        int order = 0;
        for (String l : lines) {
            String line = l == null ? "" : l.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("-")) line = line.substring(1).trim();
            line = line.replaceFirst("^\\d+[\\)\\.、]\\s*", "");
            String en = null;
            String zh = null;
            String tag = null;

            // new format: <Category> | EN: ... | ZH: ...
            Matcher m = Pattern.compile("(?is)^(.+?)\\s*\\|\\s*EN\\s*:\\s*(.+?)\\s*\\|\\s*ZH\\s*:\\s*(.+)$").matcher(line);
            if (m.find()) {
                tag = trimToNull(m.group(1));
                en = trimToNull(m.group(2));
                zh = trimToNull(m.group(3));
            } else {
                // old format: EN: ... | ZH: ...
                Matcher m2 = Pattern.compile("(?i)^EN\\s*:\\s*(.+?)(\\s*\\|\\s*ZH\\s*:\\s*(.+))?$").matcher(line);
                if (m2.find()) {
                    en = trimToNull(m2.group(1));
                    zh = trimToNull(m2.group(3));
                } else {
                    // 兼容 “英文（中文）” 样式
                    Matcher m3 = Pattern.compile("^(.+?)\\s*[（(]([^()（）]+)[)）]\\s*$").matcher(line);
                    if (m3.find()) {
                        en = trimToNull(m3.group(1));
                        zh = trimToNull(m3.group(2));
                    } else {
                        en = trimToNull(line);
                    }
                }
            }
            if (en == null) continue;
            order++;
            CardStructuredExamplePayload ep = new CardStructuredExamplePayload();
            ep.setOrder(order);
            ep.setEn(en);
            ep.setZh(zh);
            ep.setTag(tag);
            out.add(ep);
        }
        return out;
    }

    private static List<CardStructuredSynonymPayload> parseSynonyms(String block) {
        List<CardStructuredSynonymPayload> out = new ArrayList<>();
        if (block == null || block.trim().isEmpty()) return out;
        String[] lines = block.split("\\r?\\n");
        int order = 0;
        for (String l : lines) {
            String line = l == null ? "" : l.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("-")) line = line.substring(1).trim();
            line = line.replaceFirst("^\\d+[\\)\\.、]\\s*", "");

            String lemma;
            String noteZh = null;
            // 支持: lemma -> noteZh / lemma → noteZh / lemma：noteZh
            Matcher m = Pattern.compile("(?is)^(.+?)\\s*(?:->|→|:)\\s*(.+)$").matcher(line);
            if (m.find()) {
                lemma = trimToNull(m.group(1));
                noteZh = trimToNull(m.group(2));
            } else {
                lemma = trimToNull(line);
            }
            if (lemma == null || lemma.trim().isEmpty()) continue;
            order++;
            CardStructuredSynonymPayload sp = new CardStructuredSynonymPayload();
            sp.setOrder(order);
            sp.setLemma(lemma);
            sp.setNoteZh(noteZh);
            out.add(sp);
        }
        return out;
    }

    private static List<String> parseCollocations(String block) {
        List<String> out = new ArrayList<>();
        if (block == null || block.trim().isEmpty()) return out;
        String[] lines = block.split("\\r?\\n");
        for (String l : lines) {
            String line = l == null ? "" : l.trim();
            if (line.isEmpty()) continue;
            // allow bullet prefix
            if (line.startsWith("-")) line = line.substring(1).trim();
            if (line.startsWith("•")) line = line.substring(1).trim();
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    private static String extractTagBlock(String text, String tag) {
        Pattern p = Pattern.compile("(?is)\\[" + Pattern.quote(tag) + "\\](.*?)\\[/" + Pattern.quote(tag) + "\\]");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String extractByKeywordLine(String text, String keyword) {
        for (String l : text.split("\\r?\\n")) {
            String line = l == null ? "" : l.trim();
            if (line.isEmpty()) continue;
            if (!line.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) continue;
            String cleaned = line.replaceAll("(?i)^\\(?\\d+\\)?\\s*", "")
                    .replace(keyword, "")
                    .replace("：", "")
                    .replace(":", "")
                    .replace("-", "")
                    .trim();
            if (!cleaned.isEmpty()) return cleaned;
        }
        return null;
    }

    private static String extractSectionByHeader(String text, String headerKeyword) {
        String[] lines = text.split("\\r?\\n");
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim().toLowerCase(Locale.ROOT);
            if (line.contains(headerKeyword.toLowerCase(Locale.ROOT))) {
                start = i + 1;
                break;
            }
        }
        if (start < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("collocation") || lower.contains("native tip") || lower.contains("high-level")
                    || lower.contains("translation") || lower.contains("explanation")) {
                break;
            }
            sb.append(line).append("\n");
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    /**
     * 兼容旧格式里把例句写在同一行：
     * (3) "Example(s)" - sentence1 (zh) sentence2 (zh)
     */
    private static String extractExamplesInline(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?is)Example\\(s\\)[^\\n]*?-\\s*(.+)").matcher(text);
        if (!m.find()) return null;
        String tail = m.group(1).trim();
        if (tail.isEmpty()) return null;
        // 将多个句子尽量拆成多行，便于 parseExamples 处理
        tail = tail.replaceAll("(?<=\\))\\s+(?=[A-Z])", "\n");
        tail = tail.replaceAll("(?<=[.!?])\\s+(?=[A-Z])", "\n");
        return tail.trim();
    }

    private static List<String> findEnglishSentences(String text, int max) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("([A-Z][^\\n.!?]{3,}[.!?])").matcher(text);
        while (m.find() && out.size() < max) {
            String s = m.group(1).trim();
            String sl = s.toLowerCase(Locale.ROOT);
            // filter common AI labels from legacy prompts
            if (sl.contains("english definition") || sl.contains("example") || sl.contains("synonym")) {
                continue;
            }
            if (s.length() >= 6) out.add(s);
        }
        return out;
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public void syncBackContent(Long cardId) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        List<CardSenseDTO> senses = loadSensesForCard(cardId);
        CardGlobalExtraDTO global = loadGlobalForCard(cardId);
        if (senses.isEmpty() && (global == null || isGlobalEmpty(global))) {
            return;
        }
        String assembled = CardBackContentAssembler.assemble(card.getFrontContent(), senses, global);
        if (!assembled.isEmpty()) {
            card.setBackContent(assembled);
            cardRepository.save(card);
        }
    }

    private static boolean isGlobalEmpty(CardGlobalExtraDTO g) {
        boolean noCol = g.getCollocations() == null || g.getCollocations().isEmpty();
        boolean noTip = g.getNativeTip() == null || g.getNativeTip().trim().isEmpty();
        boolean noHl = (g.getHighLevelEn() == null || g.getHighLevelEn().trim().isEmpty())
                && (g.getHighLevelZh() == null || g.getHighLevelZh().trim().isEmpty());
        return noCol && noTip && noHl;
    }

    public List<CardSenseDTO> loadSensesForCard(Long cardId) {
        List<CardSense> senses = cardSenseRepository.findByCardIdOrderBySortOrderAsc(cardId);
        if (senses.isEmpty()) return Collections.emptyList();
        List<Long> senseIds = senses.stream().map(CardSense::getId).collect(Collectors.toList());
        List<CardExample> examples = cardExampleRepository.findBySenseIdIn(senseIds);
        List<CardSynonym> synonyms = cardSynonymRepository.findBySenseIdIn(senseIds);
        Map<Long, List<CardExample>> exBySense = examples.stream().collect(Collectors.groupingBy(CardExample::getSenseId));
        Map<Long, List<CardSynonym>> syBySense = synonyms.stream().collect(Collectors.groupingBy(CardSynonym::getSenseId));

        List<CardSenseDTO> out = new ArrayList<>();
        for (CardSense s : senses) {
            CardSenseDTO dto = toSenseDto(s);
            List<CardExample> exList = exBySense.getOrDefault(s.getId(), Collections.emptyList());
            exList.sort(Comparator.comparing(CardExample::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
            for (CardExample e : exList) dto.getExamples(). add(toExampleDto(e));
            List<CardSynonym> syList = syBySense.getOrDefault(s.getId(), Collections.emptyList());
            syList.sort(Comparator.comparing(CardSynonym::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
            for (CardSynonym y : syList) dto.getSynonyms().add(toSynonymDto(y));
            out.add(dto);
        }
        return out;
    }

    public CardGlobalExtraDTO loadGlobalForCard(Long cardId) {
        return cardGlobalExtraRepository.findByCardId(cardId).map(this::toGlobalDto).orElse(null);
    }

    public Map<Long, List<CardSenseDTO>> loadSensesBatch(Collection<Long> cardIds) {
        Map<Long, List<CardSenseDTO>> map = new HashMap<>();
        if (cardIds == null || cardIds.isEmpty()) return map;
        List<CardSense> allSenses = cardSenseRepository.findByCardIdIn(cardIds);
        if (allSenses.isEmpty()) return map;
        List<Long> senseIds = allSenses.stream().map(CardSense::getId).collect(Collectors.toList());
        List<CardExample> allEx = cardExampleRepository.findBySenseIdIn(senseIds);
        List<CardSynonym> allSy = cardSynonymRepository.findBySenseIdIn(senseIds);
        Map<Long, List<CardExample>> exBySense = allEx.stream().collect(Collectors.groupingBy(CardExample::getSenseId));
        Map<Long, List<CardSynonym>> syBySense = allSy.stream().collect(Collectors.groupingBy(CardSynonym::getSenseId));
        Map<Long, List<CardSense>> senseByCard = allSenses.stream().collect(Collectors.groupingBy(CardSense::getCardId));
        for (Long cid : cardIds) {
            List<CardSense> slist = senseByCard.get(cid);
            if (slist == null) continue;
            slist.sort(Comparator.comparing(CardSense::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
            List<CardSenseDTO> dtos = new ArrayList<>();
            for (CardSense s : slist) {
                CardSenseDTO dto = toSenseDto(s);
                List<CardExample> exList = new ArrayList<>(exBySense.getOrDefault(s.getId(), Collections.emptyList()));
                exList.sort(Comparator.comparing(CardExample::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
                for (CardExample e : exList) dto.getExamples().add(toExampleDto(e));
                List<CardSynonym> syList = new ArrayList<>(syBySense.getOrDefault(s.getId(), Collections.emptyList()));
                syList.sort(Comparator.comparing(CardSynonym::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
                for (CardSynonym y : syList) dto.getSynonyms().add(toSynonymDto(y));
                dtos.add(dto);
            }
            map.put(cid, dtos);
        }
        return map;
    }

    public Map<Long, CardGlobalExtraDTO> loadGlobalBatch(Collection<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return Collections.emptyMap();
        List<CardGlobalExtra> list = cardGlobalExtraRepository.findByCardIdIn(cardIds);
        Map<Long, CardGlobalExtraDTO> map = new HashMap<>();
        for (CardGlobalExtra g : list) map.put(g.getCardId(), toGlobalDto(g));
        return map;
    }

    private CardSenseDTO toSenseDto(CardSense s) {
        CardSenseDTO d = new CardSenseDTO();
        d.setId(s.getId());
        d.setCardId(s.getCardId());
        d.setSortOrder(s.getSortOrder());
        d.setLabel(s.getLabel());
        d.setTranslationZh(s.getTranslationZh());
        d.setExplanationEn(s.getExplanationEn());
        d.setTone(s.getTone());
        return d;
    }

    private CardExampleDTO toExampleDto(CardExample e) {
        CardExampleDTO d = new CardExampleDTO();
        d.setId(e.getId());
        d.setSenseId(e.getSenseId());
        d.setSortOrder(e.getSortOrder());
        d.setSentenceEn(e.getSentenceEn());
        d.setSentenceZh(e.getSentenceZh());
        d.setScenarioTag(e.getScenarioTag());
        return d;
    }

    private CardSynonymDTO toSynonymDto(CardSynonym y) {
        CardSynonymDTO d = new CardSynonymDTO();
        d.setId(y.getId());
        d.setSenseId(y.getSenseId());
        d.setSortOrder(y.getSortOrder());
        d.setLemma(y.getLemma());
        d.setNoteZh(y.getNoteZh());
        return d;
    }

    private CardGlobalExtraDTO toGlobalDto(CardGlobalExtra g) {
        CardGlobalExtraDTO d = new CardGlobalExtraDTO();
        d.setId(g.getId());
        d.setCardId(g.getCardId());
        d.setNativeTip(g.getNativeTip());
        d.setHighLevelEn(g.getHighLevelEn());
        d.setHighLevelZh(g.getHighLevelZh());
        if (g.getCollocationsJson() != null && !g.getCollocationsJson().trim().isEmpty()) {
            try {
                List<String> cols = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(g.getCollocationsJson(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                d.setCollocations(cols != null ? cols : Collections.emptyList());
            } catch (Exception ignored) {
                d.setCollocations(Collections.emptyList());
            }
        }
        return d;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
