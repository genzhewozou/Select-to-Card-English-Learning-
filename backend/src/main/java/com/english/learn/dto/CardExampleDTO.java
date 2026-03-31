package com.english.learn.dto;

import lombok.Data;

@Data
public class CardExampleDTO {
    private Long id;
    private Long senseId;
    private Integer sortOrder;
    private String sentenceEn;
    private String sentenceZh;
    private String scenarioTag;
}
