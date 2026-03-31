package com.english.learn.dto;

import lombok.Data;

@Data
public class CardSynonymDTO {
    private Long id;
    private Long senseId;
    private Integer sortOrder;
    private String lemma;
    private String noteZh;
}
