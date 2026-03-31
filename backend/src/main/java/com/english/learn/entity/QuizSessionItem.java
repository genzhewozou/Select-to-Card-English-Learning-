package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 测验会话中的单题。
 */
@Data
@Entity
@Table(name = "learn_quiz_session_item", indexes = {
        @Index(name = "idx_quiz_item_session", columnList = "session_id")
})
public class QuizSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "example_id", nullable = false)
    private Long exampleId;

    /** 题型：FRONT_INPUT / DEFINITION_INPUT / SENTENCE_TRANSLATION / SYNONYM_CHOICE */
    @Column(name = "question_type", nullable = false, length = 32)
    private String questionType;

    /** 展示用题干（可选；若为空则用 example 的 sentence_en/zh 组合） */
    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    /** 选择题选项 JSON 数组字符串，如 [\"a\",\"b\"] */
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    /** 标准答案文本（输入题为 front；选择题为正确选项文本） */
    @Column(name = "expected_text", columnDefinition = "TEXT")
    private String expectedText;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
