package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档测验会话。
 */
@Data
@Entity
@Table(name = "learn_quiz_session", indexes = {
        @Index(name = "idx_quiz_session_user_doc", columnList = "user_id,document_id")
})
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "gmt_create", nullable = false, updatable = false)
    private LocalDateTime gmtCreate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (gmtCreate == null) {
            gmtCreate = LocalDateTime.now();
        }
    }
}
