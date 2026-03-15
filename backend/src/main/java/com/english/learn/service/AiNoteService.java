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

import java.util.Collections;
import java.util.Optional;

/**
 * 调用大模型生成卡片注释（英文释义 + 中文释义 + 例句）。
 * 仅使用请求中传入的配置（页面勾选并填写），兼容 OpenAI 及任意 OpenAI 兼容接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiNoteService {

    @Qualifier("aiRestTemplate")
    private final RestTemplate aiRestTemplate;

    private static final String USER_PROMPT_TEMPLATE =
            "Generate a vocabulary note for this word/phrase. Output exactly three parts in order: "
                    + "(1) \"English definition\" — one short definition in English; "
                    + "(2) \"中文释义\" — one concise Chinese explanation; "
                    + "(3) \"Example(s)\" — 1–2 natural English sentences using the word/phrase, with optional Chinese in parentheses. "
                    + "Use the labels above. Keep the whole note under 300 words. "
                    + "Target: %s. Context (optional): %s";

    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    /**
     * 使用请求中提供的 AI 配置生成注释（页面勾选「使用 AI」并填写 Key/模型/地址时调用）。
     */
    public Optional<String> generateNoteWithConfig(
            String selectedText,
            String contextSentence,
            String apiKey,
            String model,
            String baseUrl) {
        if (selectedText == null || selectedText.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String m = (model != null && !model.isBlank()) ? model.trim() : DEFAULT_MODEL;
        String u = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        return doGenerate(selectedText, contextSentence, apiKey.trim(), m, u);
    }

    private Optional<String> doGenerate(String selectedText, String contextSentence,
                                        String apiKey, String model, String baseUrlNoTrailing) {
        String context = contextSentence != null ? contextSentence : "(none)";
        String userContent = String.format(USER_PROMPT_TEMPLATE, selectedText.trim(), context.trim());
        String url = baseUrlNoTrailing + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ChatRequest body = new ChatRequest();
        body.setModel(model);
        body.setMessages(Collections.singletonList(new ChatMessage("user", userContent)));
        body.setMax_tokens(500);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<ChatResponse> response = aiRestTemplate.exchange(
                    url, HttpMethod.POST, entity, ChatResponse.class);
            return extractContent(response.getBody());
        } catch (Exception e) {
            log.warn("AI generate note failed for target [{}]: {}", selectedText, e.getMessage());
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
