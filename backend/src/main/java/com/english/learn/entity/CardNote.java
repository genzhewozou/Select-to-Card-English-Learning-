package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 卡片注释表实体。
 * 用户对某张卡片添加的备注、释义补充等。
 */
@Data
@Entity
@Table(name = "learn_card_note", indexes = {
    @Index(name = "idx_learn_card_note_card_id", columnList = "card_id")
})
public class CardNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 卡片 ID */
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    /** 注释内容 */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

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
