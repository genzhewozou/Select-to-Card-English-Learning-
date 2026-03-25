package com.english.learn.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;

/**
 * 文档图片加载结果：资源流 + 媒体类型。
 */
@Getter
@RequiredArgsConstructor
public class DocumentImageResult {

    private final Resource resource;
    private final String contentType;
}
