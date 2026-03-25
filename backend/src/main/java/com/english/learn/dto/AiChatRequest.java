package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * AI 配置页聊天测试请求。
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "消息不能为空")
    private String message;
    @NotBlank(message = "API Key 不能为空")
    private String aiApiKey;
    private String aiModel;
    private String aiBaseUrl;
}
