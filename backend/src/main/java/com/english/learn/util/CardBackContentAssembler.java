package com.english.learn.util;

import com.english.learn.dto.CardGlobalExtraDTO;
import com.english.learn.dto.CardSenseDTO;
import java.util.List;

/**
 * 将结构化义项组装为「背面摘要文本」。
 * 方案A：背面仅保留中英释义，不包含例句/同义词/全局扩展。
 */
public final class CardBackContentAssembler {

    private CardBackContentAssembler() {
    }

    public static String assemble(String target, List<CardSenseDTO> senses, CardGlobalExtraDTO global) {
        if (senses == null || senses.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean singleSense = senses.size() == 1;

        for (CardSenseDTO s : senses) {
            if (sb.length() > 0) sb.append("\n\n");
            if (!singleSense) {
                sb.append("【释义 ").append(s.getSortOrder() != null ? s.getSortOrder() : "").append("】\n");
            }

            // 仅保留中英释义摘要
            sb.append("1️⃣ Translation into Chinese\n");
            if (notBlank(target)) {
                sb.append("“").append(target.trim()).append("” →\n");
            }
            appendBulletLines(sb, s.getTranslationZh());

            sb.append("\n2️⃣ Explanation in English\n");
            if (notBlank(s.getExplanationEn())) {
                sb.append(s.getExplanationEn().trim()).append("\n");
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

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
