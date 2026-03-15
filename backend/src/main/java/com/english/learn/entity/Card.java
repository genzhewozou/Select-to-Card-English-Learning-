package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 卡片表实体。
 * 由用户在文档中选中的单词或句子生成，可关联文档与位置信息。
 */
@Data
@Entity
@Table(name = "learn_card", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_document_id", columnList = "document_id")
})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 来源文档 ID，可为空（手动创建的卡片） */
    @Column(name = "document_id")
    private Long documentId;

    /** 正面内容：单词或句子原文 */
    @Column(name = "front_content", nullable = false, columnDefinition = "TEXT")
    private String frontContent;

    /** 背面内容：释义、例句等，可选 */
    @Column(name = "back_content", columnDefinition = "TEXT")
    private String backContent;

    /** 在文档中的起始偏移（用于高亮定位），可选 */
    @Column(name = "start_offset")
    private Integer startOffset;

    /** 在文档中的结束偏移，可选 */
    @Column(name = "end_offset")
    private Integer endOffset;

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
