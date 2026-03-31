package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 义项下的同义词（无例句，仅展示）。
 */
@Data
@Entity
@Table(name = "learn_card_synonym", indexes = {
        @Index(name = "idx_learn_card_synonym_sense_id", columnList = "sense_id")
})
public class CardSynonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sense_id", nullable = false)
    private Long senseId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "lemma", nullable = false, columnDefinition = "TEXT")
    private String lemma;

    @Column(name = "note_zh", columnDefinition = "TEXT")
    private String noteZh;

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
