package com.english.learn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文档原件本地存储根目录（相对进程工作目录或绝对路径）。
 * 部署在自有服务器上时建议设为绝对路径，例如 /var/english-learn/documents。
 */
@Data
@ConfigurationProperties(prefix = "english-learn.document")
public class DocumentStorageProperties {

    /**
     * 存储根目录，默认 ./data/documents
     */
    private String storageRoot = "./data/documents";
}
