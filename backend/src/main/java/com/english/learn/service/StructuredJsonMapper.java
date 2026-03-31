package com.english.learn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.english.learn.dto.structured.*;

import java.util.List;

/**
 * 将 AI 返回的 JSON（schema v1）转为保存请求 DTO。
 */
public final class StructuredJsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StructuredJsonMapper() {
    }

    public static CardStructuredSaveRequest parse(String json) {
        String text = stripCodeFence(json);
        JsonNode root;
        try {
            root = MAPPER.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage());
        }
        if (!root.has("schemaVersion") || root.get("schemaVersion").asInt() != 1) {
            throw new IllegalArgumentException("仅支持 schemaVersion=1");
        }
        JsonNode sensesNode = root.get("senses");
        if (sensesNode == null || !sensesNode.isArray() || sensesNode.isEmpty()) {
            throw new IllegalArgumentException("senses 不能为空");
        }
        CardStructuredSaveRequest req = new CardStructuredSaveRequest();
        int idx = 0;
        for (JsonNode sn : sensesNode) {
            idx++;
            CardStructuredSensePayload sp = new CardStructuredSensePayload();
            sp.setOrder(sn.has("order") ? sn.get("order").asInt() : idx);
            sp.setLabel(textOrNull(sn, "label"));
            sp.setTranslationZh(textOrNull(sn, "translationZh"));
            sp.setExplanationEn(textOrNull(sn, "explanationEn"));
            sp.setTone(textOrNull(sn, "tone"));
            if (sn.has("examples") && sn.get("examples").isArray()) {
                int exOrder = 0;
                for (JsonNode en : sn.get("examples")) {
                    exOrder++;
                    String enText = textOrNull(en, "en");
                    if (enText == null || enText.isEmpty()) continue;
                    CardStructuredExamplePayload ep = new CardStructuredExamplePayload();
                    ep.setOrder(en.has("order") ? en.get("order").asInt() : exOrder);
                    ep.setEn(enText);
                    ep.setZh(textOrNull(en, "zh"));
                    ep.setTag(textOrNull(en, "tag"));
                    sp.getExamples().add(ep);
                }
            }
            if (sn.has("synonyms") && sn.get("synonyms").isArray()) {
                int syOrder = 0;
                for (JsonNode syn : sn.get("synonyms")) {
                    syOrder++;
                    String lemma = textOrNull(syn, "lemma");
                    if (lemma == null || lemma.isEmpty()) continue;
                    CardStructuredSynonymPayload yp = new CardStructuredSynonymPayload();
                    yp.setOrder(syn.has("order") ? syn.get("order").asInt() : syOrder);
                    yp.setLemma(lemma);
                    yp.setNoteZh(textOrNull(syn, "noteZh"));
                    sp.getSynonyms().add(yp);
                }
            }
            req.getSenses().add(sp);
        }
        JsonNode global = root.get("globalSections");
        if (global != null && !global.isNull()) {
            CardStructuredGlobalPayload gp = new CardStructuredGlobalPayload();
            if (global.has("collocations") && global.get("collocations").isArray()) {
                for (JsonNode c : global.get("collocations")) {
                    if (c.isTextual()) gp.getCollocations().add(c.asText());
                }
            }
            gp.setNativeTip(textOrNull(global, "nativeTip"));
            if (global.has("highLevelExample") && global.get("highLevelExample").isObject()) {
                JsonNode h = global.get("highLevelExample");
                gp.setHighLevelEn(textOrNull(h, "en"));
                gp.setHighLevelZh(textOrNull(h, "zh"));
            }
            req.setGlobalExtra(gp);
        }
        return req;
    }

    private static String textOrNull(JsonNode parent, String field) {
        if (parent == null || !parent.has(field) || parent.get(field).isNull()) return null;
        JsonNode n = parent.get(field);
        if (n.isTextual()) return n.asText();
        if (n.isNumber()) return n.asText();
        return n.toString();
    }

    private static String stripCodeFence(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            } else {
                s = s.substring(3);
            }
            int fence = s.lastIndexOf("```");
            if (fence >= 0) s = s.substring(0, fence);
        }
        return s.trim();
    }
}
