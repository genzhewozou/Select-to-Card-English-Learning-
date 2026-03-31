package com.english.learn.dto.quiz;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QuizAnswerRequest {
    @NotNull
    private Long itemId;
    /** 用户填写的英文（通常对应卡片正面） */
    private String answer;
    /** 组合题第二部分答案（中文句子的英文翻译） */
    private String answerExtra;
    /** 句子题可选启用 AI 判分（不落库） */
    private String aiApiKey;
    private String aiModel;
    private String aiBaseUrl;
}
