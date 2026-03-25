package com.english.learn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档列表用投影：不含正文 content，减轻查询与传输。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryDTO {

    private Long id;
    private Long userId;
    private String fileName;
    private String fileType;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private String storedFilePath;
}
