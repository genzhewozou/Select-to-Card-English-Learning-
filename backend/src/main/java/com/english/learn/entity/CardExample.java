package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 义项下的例句（文档测验题源）。
 */
@Data
@Entity
@Table(name = "learn_card_example", indexes = {
        @Index(name = "idx_learn_card_example_sense_id", columnList = "sense_id")
})
public class CardExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sense_id", nullable = false)
    private Long senseId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "sentence_en", nullable = false, columnDefinition = "TEXT")
    private String sentenceEn;

    @Column(name = "sentence_zh", columnDefinition = "TEXT")
    private String sentenceZh;

    @Column(name = "scenario_tag", columnDefinition = "TEXT")
    private String scenarioTag;

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
