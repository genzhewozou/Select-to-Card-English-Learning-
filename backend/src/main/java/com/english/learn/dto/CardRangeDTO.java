package com.english.learn.dto;

import lombok.Data;

/**
 * 文档内高亮用的卡片范围（轻量）。
 */
@Data
public class CardRangeDTO {
    private Long id;
    private Integer startOffset;
    private Integer endOffset;
    private String frontContent;
}

