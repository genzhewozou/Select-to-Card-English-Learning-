package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.AiGenerateNoteRequest;
import com.english.learn.service.AiNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

/**
 * AI 相关接口：仅生成注释预览（不创建卡片）。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiNoteService aiNoteService;

    /** POST /api/ai/generate-note：仅生成注释文本，供弹窗内预览/编辑 */
    @PostMapping("/generate-note")
    public Result<String> generateNote(@Valid @RequestBody AiGenerateNoteRequest req) {
        Optional<String> content = aiNoteService.generateNoteWithConfig(
                req.getFrontContent(),
                req.getContextSentence(),
                req.getAiApiKey(),
                req.getAiModel(),
                req.getAiBaseUrl());
        return Result.success(content.orElse(""));
    }
}
