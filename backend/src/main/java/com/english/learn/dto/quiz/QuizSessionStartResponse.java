package com.english.learn.dto.quiz;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuizSessionStartResponse {
    private Long sessionId;
    private List<QuizQuestionDTO> questions = new ArrayList<>();
}
