package com.english.learn.dto.structured;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CardStructuredGenerateRequest {
    /** 文档上下文句，帮助消歧 */
    private String contextSentence;
    @NotBlank
    private String aiApiKey;
    private String aiModel;
    private String aiBaseUrl;
}
