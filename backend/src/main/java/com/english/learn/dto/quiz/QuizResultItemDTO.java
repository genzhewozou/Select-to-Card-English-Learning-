package com.english.learn.dto.quiz;

import lombok.Data;

@Data
public class QuizResultItemDTO {
    private Long itemId;
    private Integer sequence;
    private String type;
    private String prompt;
    private String sentenceEn;
    private Boolean isCorrect;
    private String expected;
    private String userAnswer;
}
