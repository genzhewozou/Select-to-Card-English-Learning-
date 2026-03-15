package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 卡片注释 DTO。
 */
@Data
public class CardNoteDTO {

    private Long id;
    @NotNull
    private Long cardId;
    @NotBlank(message = "注释内容不能为空")
    private String content;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
