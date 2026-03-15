package com.english.learn.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡片请求/响应 DTO。
 */
@Data
public class CardDTO {

    private Long id;
    @NotNull
    private Long userId;
    private Long documentId;
    @NotBlank(message = "正面内容不能为空")
    private String frontContent;
    private String backContent;
    /** 选中内容所在的句子或上下文，用于 AI 生成注释时消歧与生成更贴切例句（选填） */
    private String contextSentence;
    /** 是否使用 AI 生成注释（由前端勾选，勾选时需传 aiApiKey 等） */
    private Boolean useAiNote;
    /** AI 模型 API Key（仅当 useAiNote=true 时使用，不落库） */
    private String aiApiKey;
    /** AI 模型名称，如 gpt-3.5-turbo（选填，默认 gpt-3.5-turbo） */
    private String aiModel;
    /** AI 接口根地址，如 https://api.openai.com/v1（选填） */
    private String aiBaseUrl;
    /** 前端已生成好的注释内容（有此字段时直接落库，不再调 AI） */
    private String aiNoteContent;
    private Integer startOffset;
    private Integer endOffset;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    /** 注释列表（响应时填充） */
    private List<CardNoteDTO> notes;
    /** 学习进度（响应时填充） */
    private CardProgressDTO progress;
}
