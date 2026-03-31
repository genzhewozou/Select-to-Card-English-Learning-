package com.english.learn.dto.quiz;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuizResultResponse {
    private Long sessionId;
    private int total;
    private int correctCount;
    private List<QuizResultItemDTO> items = new ArrayList<>();
}
