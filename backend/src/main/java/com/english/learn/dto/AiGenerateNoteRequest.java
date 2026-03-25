package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 仅生成 AI 注释的请求（不创建卡片）。
 */
@Data
public class AiGenerateNoteRequest {

    @NotBlank(message = "正面内容不能为空")
    private String frontContent;
    private String contextSentence;
    @NotBlank(message = "API Key 不能为空")
    private String aiApiKey;
    private String aiModel;
    private String aiBaseUrl;
    /** 可配置的注释生成提示词模板，支持 {{target}} / {{context}} 占位符 */
    private String aiNotePrompt;
}
