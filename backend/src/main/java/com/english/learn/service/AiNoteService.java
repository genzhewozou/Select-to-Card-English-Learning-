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
import java.util.Collections;
import java.util.List;
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

    public static final String DEFAULT_NOTE_PROMPT_TEMPLATE =
            "You are generating structured vocabulary learning content for a flashcard.\n"
                    + "Return ONLY tagged blocks (no markdown code fences, no extra commentary). You MUST match tags exactly.\n"
                    + "\n"
                    + "IMPORTANT: If TARGET has multiple distinct meanings/usages (e.g. noun vs verb, different Chinese translations), you MUST output multiple [SENSE] blocks. One [SENSE] block = one meaning.\n"
                    + "Inside each [SENSE] block, translationZh MUST be exactly ONE Chinese meaning (single line; NO bullets; NO multiple meanings).\n"
                    + "Examples and synonyms inside a [SENSE] block must ONLY match that sense.\n"
                    + "\n"
                    + "[SENSE]\n"
                    + "[TRANSLATION_ZH]\n"
                    + "One-line Chinese meaning only.\n"
                    + "[/TRANSLATION_ZH]\n"
                    + "\n"
                    + "[EXPLANATION_EN]\n"
                    + "Write in English with these exact subheadings and emojis:\n"
                    + "✅ Meaning\n"
                    + "...\n"
                    + "✅ Tone\n"
                    + "...\n"
                    + "[/EXPLANATION_EN]\n"
                    + "\n"
                    + "[EXAMPLES]\n"
                    + "Provide 2-4 lines. Each line MUST match:\n"
                    + "- <Category> | EN: <english sentence> | ZH: <chinese translation>\n"
                    + "Category examples: Economic / Policy, Educational / Mental, Scientific / Physical, IELTS-style / Academic, etc.\n"
                    + "[/EXAMPLES]\n"
                    + "\n"
                    + "[SYNONYMS]\n"
                    + "Provide 3-6 synonym lines. Each line MUST match:\n"
                    + "- <synonym phrase> -> <chinese meaning>\n"
                    + "Only synonyms; NO separate example for synonyms.\n"
                    + "[/SYNONYMS]\n"
                    + "[/SENSE]\n"
                    + "\n"
                    + "[NATIVE_TIP]\n"
                    + "Write a short native usage tip in English.\n"
                    + "[/NATIVE_TIP]\n"
                    + "\n"
                    + "[HIGH_LEVEL_EN]\n"
                    + "Write 1 high-level English sentence.\n"
                    + "[/HIGH_LEVEL_EN]\n"
                    + "\n"
                    + "[HIGH_LEVEL_ZH]\n"
                    + "Write the Chinese translation of the high-level sentence.\n"
                    + "[/HIGH_LEVEL_ZH]\n"
                    + "\n"
                    + "Target: {{target}}.\n"
                    + "Context (optional): {{context}}";

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
            String baseUrl,
            String notePromptTemplate) {
        if (selectedText == null || selectedText.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }
        String m = (model != null && !model.trim().isEmpty()) ? model.trim() : DEFAULT_MODEL;
        String u = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        return doGenerate(selectedText, contextSentence, apiKey.trim(), m, u, notePromptTemplate);
    }

    private Optional<String> doGenerate(String selectedText, String contextSentence,
                                        String apiKey, String model, String baseUrlNoTrailing,
                                        String notePromptTemplate) {
        String context = contextSentence != null ? contextSentence : "(none)";
        String userContent = buildNotePrompt(notePromptTemplate, selectedText.trim(), context.trim());
        String url = baseUrlNoTrailing + "/chat/completions";
        // 需要输出多块结构化内容，500 tokens 很容易截断导致缺块/缺同义词/缺中文
        return sendChat(url, apiKey, model, Collections.singletonList(new ChatMessage("user", userContent)), 1600);
    }

    public Optional<String> chatWithConfig(String userMessage, String apiKey, String model, String baseUrl) {
        if (userMessage == null || userMessage.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }
        String m = (model != null && !model.trim().isEmpty()) ? model.trim() : DEFAULT_MODEL;
        String u = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
        String url = u + "/chat/completions";
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", userMessage.trim()));
        return sendChat(url, apiKey.trim(), m, messages, 800);
    }

    private Optional<String> sendChat(String url, String apiKey, String model, List<ChatMessage> messages, Integer maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

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
            log.warn("AI chat request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildNotePrompt(String template, String target, String context) {
        // 若用户自定义 prompt 不包含我们约定的块标签，则回退到内置模板，保证可解析性
        String userTpl = template != null ? template.trim() : "";
        boolean looksTagged = userTpl.contains("[TRANSLATION_ZH]") && userTpl.contains("[EXAMPLES]") && userTpl.contains("[SYNONYMS]");
        String t = (!userTpl.isEmpty() && looksTagged) ? userTpl : DEFAULT_NOTE_PROMPT_TEMPLATE;
        if (t.contains("{{target}}") || t.contains("{{context}}")) {
            return t.replace("{{target}}", target).replace("{{context}}", context);
        }
        if (t.contains("%s")) {
            try {
                return String.format(t, target, context);
            } catch (Exception ignored) {
                // ignore formatting mismatch and fallback below
            }
        }
        return t + "\nTarget: " + target + "\nContext: " + context;
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
