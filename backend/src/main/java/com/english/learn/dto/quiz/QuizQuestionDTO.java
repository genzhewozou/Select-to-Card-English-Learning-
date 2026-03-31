package com.english.learn.dto.quiz;

import lombok.Data;

@Data
public class QuizQuestionDTO {
    private Long itemId;
    private Integer sequence;
    /** 题型：COMBINED_INPUT / FRONT_INPUT / DEFINITION_INPUT / SENTENCE_TRANSLATION / SYNONYM_CHOICE */
    private String type;
    /** 展示用题干 */
    private String prompt;
    /** 可选：例句英文 */
    private String sentenceEn;
    /** 可选：例句中文 */
    private String sentenceZh;
    /** 选择题选项；输入题为空 */
    private java.util.List<String> options;
}
