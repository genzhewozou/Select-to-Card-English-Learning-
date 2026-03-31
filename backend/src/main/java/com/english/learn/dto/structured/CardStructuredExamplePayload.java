package com.english.learn.dto.structured;

import lombok.Data;

@Data
public class CardStructuredExamplePayload {
    private Integer order;
    /** 空行在服务层跳过不落库 */
    private String en;
    private String zh;
    private String tag;
}
