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
    /** 是否已在服务器磁盘保存上传原件（可调用下载接口） */
    private Boolean originalAvailable;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
