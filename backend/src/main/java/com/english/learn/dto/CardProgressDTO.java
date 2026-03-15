package com.english.learn.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习进度 DTO。
 */
@Data
public class CardProgressDTO {

    private Long id;
    private Long userId;
    private Long cardId;
    /** 熟练度 1-5 */
    private Integer proficiencyLevel;
    private Integer reviewCount;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewAt;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
