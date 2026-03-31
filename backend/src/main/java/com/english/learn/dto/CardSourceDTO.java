package com.english.learn.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardSourceDTO {
    private Long sourceId;
    private Long cardId;
    private Long documentId;
    private String documentName;
    private Integer startOffset;
    private Integer endOffset;
    private LocalDateTime gmtCreate;
}
