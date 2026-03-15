package com.english.learn.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Integer max_tokens = 500;
}
