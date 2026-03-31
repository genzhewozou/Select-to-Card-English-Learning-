package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 卡片级扩展块：搭配、提示、高水平例句（JSON 由 collocations 列存储数组字符串）。
 */
@Data
@Entity
@Table(name = "learn_card_global_extra", uniqueConstraints = @UniqueConstraint(name = "uk_global_extra_card", columnNames = "card_id"))
public class CardGlobalExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false, unique = true)
    private Long cardId;

    /** JSON 数组字符串，如 ["a","b"] */
    @Column(name = "collocations_json", columnDefinition = "TEXT")
    private String collocationsJson;

    @Column(name = "native_tip", columnDefinition = "TEXT")
    private String nativeTip;

    @Column(name = "high_level_en", columnDefinition = "TEXT")
    private String highLevelEn;

    @Column(name = "high_level_zh", columnDefinition = "TEXT")
    private String highLevelZh;

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
