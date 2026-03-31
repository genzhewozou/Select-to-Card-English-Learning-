package com.english.learn.dto.quiz;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QuizSessionStartRequest {
    @NotNull
    private Long documentId;
    /** 默认 10，最大 50 */
    private Integer questionCount;
    /** 是否优先错题（默认 true） */
    private Boolean prioritizeWrong;
    /** 是否优先低熟练度（默认 true） */
    private Boolean prioritizeLowProficiency;
    /** 测试中是否启用 AI 临时例句（仅会话内） */
    private Boolean useAiTempExamples;
    private String aiApiKey;
    private String aiModel;
    private String aiBaseUrl;
}
