package com.english.learn.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

/**
 * 文档原件下载：文件名 + 资源流。
 */
@Getter
@RequiredArgsConstructor
public class DocumentDownloadResult {

    private final String fileName;
    private final Resource resource;
}
