package com.english.learn.service;

import com.english.learn.dto.ai.ChatMessage;
import com.english.learn.dto.ai.ChatRequest;
import com.english.learn.dto.ai.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 内置提示词：要求模型仅输出 schemaVersion=1 的 JSON，用户不可配置格式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiStructuredCardService {

    @Qualifier("aiRestTemplate")
    private final RestTemplate aiRestTemplate;

    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private static final String SYSTEM_PROMPT = "You are a vocabulary structuring API. "
            + "Output ONLY a single valid JSON object, no markdown, no code fences, no commentary. "
            + "The JSON must follow this exact structure:\n"
            + "{\n"
            + "  \"schemaVersion\": 1,\n"
            + "  \"senses\": [ {\n"
            + "    \"order\": 1,\n"
            + "    \"translationZh\": \"ONE Chinese meaning for this sense (single meaning, not a list)\",\n"
            + "    \"explanationEn\": \"English explanation\",\n"
            + "    \"examples\": [ { \"order\": 1, \"en\": \"English sentence using the target\", "
            + "\"zh\": \"optional Chinese\" } ],\n"
            + "    \"synonyms\": [ { \"order\": 1, \"lemma\": \"synonym phrase\" } ]\n"
            + "  } ],\n"
            + "  \"globalSections\": {\n"
            + "    \"nativeTip\": \"optional short tip or empty string\",\n"
            + "    \"highLevelExample\": { \"en\": \"\", \"zh\": \"\" }\n"
            + "  }\n"
            + "}\n"
            + "Rules:\n"
            + "- Use at least one sense.\n"
            + "- If the TARGET has multiple distinct meanings/usages (e.g. noun vs verb, different translations), you MUST split them into multiple objects in senses[].\n"
            + "- For each sense, translationZh MUST be exactly ONE meaning (a single Chinese phrase), not multiple bullet points or multiple lines.\n"
            + "- Keep the examples/synonyms strictly aligned to that one sense; do NOT mix examples/synonyms across senses.\n"
            + "- Each sense should have at least one natural example sentence in English that uses the TARGET exactly as students must recall it (the target is the card front).\n"
            + "- synonyms have NO separate example field.\n"
            + "- nativeTip is OPTIONAL. If no high-value concise tip, return empty string.\n"
            + "- For now, DO NOT generate high-level example; set highLevelExample.en and highLevelExample.zh to empty strings.\n"
            + "- Keep JSON compact but complete.";

    public Optional<String> generateStructuredJson(String target, String context,
                                                   String apiKey, String model, String baseUrl) {
        if (target == null || target.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }
        String m = (model != null && !model.trim().isEmpty()) ? model.trim() : DEFAULT_MODEL;
        String u = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        String ctx = (context != null && !context.trim().isEmpty()) ? context.trim() : "(none)";
        String userMsg = "TARGET (card front, exact string users learn): " + target.trim() + "\n"
                + "CONTEXT sentence from document (may help disambiguate): " + ctx + "\n"
                + "Produce the JSON now.";
        String url = u + "/chat/completions";
        return sendChat(url, apiKey.trim(), m, userMsg, 4000);
    }

    private Optional<String> sendChat(String url, String apiKey, String model, String userContent, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));
        messages.add(new ChatMessage("user", userContent));

        ChatRequest body = new ChatRequest();
        body.setModel(model);
        body.setMessages(messages);
        body.setMax_tokens(maxTokens);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<ChatResponse> response = aiRestTemplate.exchange(
                    url, HttpMethod.POST, entity, ChatResponse.class);
            return extractContent(response.getBody());
        } catch (Exception e) {
            log.warn("AI structured request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractContent(ChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return Optional.empty();
        }
        ChatMessage message = response.getChoices().get(0).getMessage();
        if (message == null || message.getContent() == null) {
            return Optional.empty();
        }
        String content = message.getContent().trim();
        return content.isEmpty() ? Optional.empty() : Optional.of(content);
    }

}
