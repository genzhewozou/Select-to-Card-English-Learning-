package com.english.learn.util;

import com.english.learn.dto.CardExampleDTO;
import com.english.learn.dto.CardGlobalExtraDTO;
import com.english.learn.dto.CardSenseDTO;
import com.english.learn.dto.CardSynonymDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将义项树组装为背面纯文本（用于搜索、复习展示同步）。
 */
public final class CardBackContentAssembler {

    private CardBackContentAssembler() {
    }

    public static String assemble(String target, List<CardSenseDTO> senses, CardGlobalExtraDTO global) {
        if (senses == null || senses.isEmpty()) {
            return global == null ? "" : appendGlobal(global).trim();
        }

        StringBuilder sb = new StringBuilder();
        boolean singleSense = senses.size() == 1;

        for (CardSenseDTO s : senses) {
            if (sb.length() > 0) sb.append("\n\n");
            if (!singleSense) {
                sb.append("【释义 ").append(s.getSortOrder() != null ? s.getSortOrder() : "").append("】\n");
            }

            // 1️⃣ Translation into Chinese
            sb.append("1️⃣ Translation into Chinese\n");
            if (notBlank(target)) {
                sb.append("“").append(target.trim()).append("” →\n");
            }
            appendBulletLines(sb, s.getTranslationZh());

            // 2️⃣ Explanation in English
            sb.append("\n2️⃣ Explanation in English\n");
            if (notBlank(s.getExplanationEn())) {
                sb.append(s.getExplanationEn().trim()).append("\n");
            }

            // 3️⃣ Examples
            sb.append("\n3️⃣ Examples\n");
            appendExamples(sb, s.getExamples());

            // 4️⃣ Synonyms / Related Words
            sb.append("\n4️⃣ Synonyms / Related Words\n");
            appendSynonyms(sb, s.getSynonyms());
        }

        // 5️⃣+ are global (word-level)
        if (global != null) {
            String g = appendGlobal(global);
            if (!g.trim().isEmpty()) {
                // 只空一行（避免出现两行空白）
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append("\n");
                sb.append("\n").append(g.trim());
            }
        }

        return sb.toString().trim();
    }

    private static void appendBulletLines(StringBuilder sb, String raw) {
        if (!notBlank(raw)) {
            return;
        }
        String[] lines = raw.split("\\r?\\n");
        for (String l : lines) {
            String line = l == null ? "" : l.trim();
            if (line.isEmpty()) continue;
            // allow already-bulleted content
            if (line.startsWith("•")) line = line.substring(1).trim();
            if (line.startsWith("-")) line = line.substring(1).trim();
            if (!line.isEmpty()) {
                sb.append("• ").append(line).append("\n");
            }
        }
    }

    private static void appendExamples(StringBuilder sb, List<com.english.learn.dto.CardExampleDTO> examples) {
        if (examples == null || examples.isEmpty()) {
            sb.append("（暂无例句）\n");
            return;
        }
        // Group by scenarioTag (category)
        Map<String, List<com.english.learn.dto.CardExampleDTO>> byTag = new LinkedHashMap<>();
        for (com.english.learn.dto.CardExampleDTO ex : examples) {
            if (ex == null) continue;
            String tag = notBlank(ex.getScenarioTag()) ? ex.getScenarioTag().trim() : "Example(s)";
            byTag.computeIfAbsent(tag, k -> new java.util.ArrayList<>()).add(ex);
        }

        for (Map.Entry<String, List<com.english.learn.dto.CardExampleDTO>> e : byTag.entrySet()) {
            // 用独立图标，避免与 1️⃣2️⃣3️⃣4️⃣ 主区块混淆
            sb.append("🔹 ").append(e.getKey()).append("\n");
            for (com.english.learn.dto.CardExampleDTO ex : e.getValue()) {
                if (notBlank(ex.getSentenceEn())) {
                    sb.append(ex.getSentenceEn().trim()).append("\n");
                }
                if (notBlank(ex.getSentenceZh())) {
                    sb.append(ex.getSentenceZh().trim()).append("\n");
                }
            }
        }
    }

    private static void appendSynonyms(StringBuilder sb, List<CardSynonymDTO> synonyms) {
        if (synonyms == null || synonyms.isEmpty()) {
            sb.append("（暂无同义词）\n");
            return;
        }
        boolean any = false;
        for (CardSynonymDTO sy : synonyms) {
            if (!notBlank(sy.getLemma())) continue;
            any = true;
            sb.append("• ").append(sy.getLemma().trim());
            if (notBlank(sy.getNoteZh())) {
                sb.append(" → ").append(sy.getNoteZh().trim());
            }
            sb.append("\n");
        }
        if (!any) sb.append("（暂无同义词）\n");
    }

    private static String appendGlobal(CardGlobalExtraDTO g) {
        if (g == null) return "";
        StringBuilder sb = new StringBuilder();

        if (notBlank(g.getNativeTip())) {
            sb.append("5️⃣ Native Tip\n");
            sb.append(g.getNativeTip().trim()).append("\n");
        }

        if (notBlank(g.getHighLevelEn()) || notBlank(g.getHighLevelZh())) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("6️⃣ High-level Sentence (Professional / IELTS / Motivational)\n");
            if (notBlank(g.getHighLevelEn())) sb.append(g.getHighLevelEn().trim()).append("\n");
            if (notBlank(g.getHighLevelZh())) sb.append(g.getHighLevelZh().trim()).append("\n");
        }

        return sb.toString();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
