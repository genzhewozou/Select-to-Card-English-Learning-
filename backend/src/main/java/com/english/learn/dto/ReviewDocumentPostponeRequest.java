package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 文档级延后复习请求：将某文档下全部卡片的复习时间延后指定天数。
 */
@Data
public class ReviewDocumentPostponeRequest {

    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    /** 支持 1/2/7 天 */
    @NotNull(message = "延后天数不能为空")
    @Min(value = 1, message = "延后天数最少为1天")
    @Max(value = 7, message = "延后天数最多为7天")
    private Integer days;
}
