package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 复习提交请求：标记熟练度后更新下次复习时间。
 */
@Data
public class ReviewRequest {

    @NotNull(message = "卡片ID不能为空")
    private Long cardId;
    /** 熟练度 1-5，用于艾宾浩斯间隔计算 */
    @Min(1)
    @Max(5)
    private Integer proficiencyLevel;
}
