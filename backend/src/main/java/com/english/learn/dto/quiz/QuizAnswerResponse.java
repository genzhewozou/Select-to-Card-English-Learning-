package com.english.learn.dto.quiz;

import lombok.Data;

@Data
public class QuizAnswerResponse {
    private boolean correct;
    /** CORRECT / PARTIAL / WRONG */
    private String verdict;
    /** 0-100 */
    private Integer score;
    /** 题后简短反馈 */
    private String feedback;
    private String expectedFront;
    private String expectedSentence;
    private String sentenceEn;
    private String sentenceZh;
    /** 组合题：上半题词条是否正确 */
    private Boolean frontCorrect;
    /** 组合题：下半题句子判定 CORRECT / PARTIAL / WRONG */
    private String sentenceVerdict;
    /** 组合题：下半题句子分数 0-100 */
    private Integer sentenceScore;
    /** 组合题：上半题反馈 */
    private String frontFeedback;
    /** 组合题：下半题反馈 */
    private String sentenceFeedback;
    private boolean sessionCompleted;
}
