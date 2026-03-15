package com.english.learn.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档请求/响应 DTO。
 */
@Data
public class DocumentDTO {

    private Long id;
    private Long userId;
    private String fileName;
    private String fileType;
    private String content;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
