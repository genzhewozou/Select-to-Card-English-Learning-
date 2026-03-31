package com.english.learn.dto.structured;

import lombok.Data;

@Data
public class CardStructuredSynonymPayload {
    private Integer order;
    /** 空行跳过 */
    private String lemma;
    private String noteZh;
}
