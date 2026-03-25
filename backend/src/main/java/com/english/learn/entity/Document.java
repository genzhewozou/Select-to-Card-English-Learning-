package com.english.learn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档表实体。
 * 存储用户上传的英语文档（Word/TXT）元信息及解析后的纯文本内容。
 */
@Data
@Entity
@Table(name = "learn_document", indexes = {
    @Index(name = "idx_learn_document_user_create_id", columnList = "user_id, gmt_create, id")
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 原始文件名 */
    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    /** 文件类型：doc/docx/txt */
    @Column(name = "file_type", length = 16)
    private String fileType;

    /** 解析后的纯文本内容，用于前端展示与选词 */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 上传原件在磁盘上的相对路径（相对 english-learn.document.storage-root），
     * 为空表示历史数据或仅内存解析未落盘原件。
     */
    @Column(name = "stored_file_path", length = 512)
    private String storedFilePath;

    @Column(name = "gmt_create", nullable = false, updatable = false)
    private LocalDateTime gmtCreate;

    @Column(name = "gmt_modified", nullable = false)
    private LocalDateTime gmtModified;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.gmtCreate = now;
        this.gmtModified = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.gmtModified = LocalDateTime.now();
    }
}
