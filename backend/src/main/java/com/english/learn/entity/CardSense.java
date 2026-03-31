package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 卡片义项（一词多义中的一条）。
 */
@Data
@Entity
@Table(name = "learn_card_sense", indexes = {
        @Index(name = "idx_learn_card_sense_card_id", columnList = "card_id")
})
public class CardSense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "label", columnDefinition = "TEXT")
    private String label;

    @Column(name = "translation_zh", columnDefinition = "TEXT")
    private String translationZh;

    @Column(name = "explanation_en", columnDefinition = "TEXT")
    private String explanationEn;

    @Column(name = "tone", columnDefinition = "TEXT")
    private String tone;

    @Column(name = "gmt_create", nullable = false, updatable = false)
    private LocalDateTime gmtCreate;

    @Column(name = "gmt_modified", nullable = false)
    private LocalDateTime gmtModified;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.gmtCreate = now;
        this.gmtModified = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.gmtModified = LocalDateTime.now();
    }
}
