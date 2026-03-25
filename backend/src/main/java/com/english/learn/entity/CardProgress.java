package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 学习进度表实体。
 * 记录每张卡片的复习次数、熟练度、下次复习时间（艾宾浩斯）。
 */
@Data
@Entity
@Table(name = "learn_card_progress", indexes = {
    @Index(name = "idx_learn_card_progress_card_id", columnList = "card_id", unique = true),
    @Index(name = "idx_learn_card_progress_user_next", columnList = "user_id, next_review_at")
})
public class CardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 卡片 ID */
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    /** 熟练度：1-5，数字越大越熟 */
    @Column(name = "proficiency_level")
    private Integer proficiencyLevel;

    /** 复习次数 */
    @Column(name = "review_count")
    private Integer reviewCount = 0;

    /** 下次复习时间（艾宾浩斯计算） */
    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    /** 上次复习时间 */
    @Column(name = "last_review_at")
    private LocalDateTime lastReviewAt;

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
