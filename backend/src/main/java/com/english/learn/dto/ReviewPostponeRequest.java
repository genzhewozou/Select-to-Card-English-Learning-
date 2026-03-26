package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 复习延后请求：将某张卡片的下次复习时间延后指定天数。
 */
@Data
public class ReviewPostponeRequest {

    @NotNull(message = "卡片ID不能为空")
    private Long cardId;

    /** 支持 1/2/7 天 */
    @NotNull(message = "延后天数不能为空")
    @Min(value = 1, message = "延后天数最少为1天")
    @Max(value = 7, message = "延后天数最多为7天")
    private Integer days;
}
