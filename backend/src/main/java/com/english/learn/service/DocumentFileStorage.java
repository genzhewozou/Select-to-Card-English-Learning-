package com.english.learn.service;

import com.english.learn.config.DocumentStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 将上传的 Word/TXT 原件保存到本机磁盘，数据库只存相对路径。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFileStorage {

    private final DocumentStorageProperties properties;

    public Path resolveRoot() {
        return Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
    }

    /**
     * 将临时文件移动到正式目录，返回相对 {@link #resolveRoot()} 的路径，用正斜杠分隔。
     */
    public String storeUploadedFile(Long userId, Long docId, String originalFileName, Path tempFile) throws IOException {
        Path root = resolveRoot();
        String safe = sanitizeFileName(originalFileName);
        Path dir = resolveDocDir(userId, docId);
        Files.createDirectories(dir);
        Path dest = dir.resolve(safe);
        Files.move(tempFile, dest, StandardCopyOption.REPLACE_EXISTING);
        return userId + "/" + docId + "/" + safe;
    }

    public Path resolveDocDir(Long userId, Long docId) {
        return resolveRoot().resolve(String.valueOf(userId)).resolve(String.valueOf(docId));
    }

    public void storeDocImage(Long userId, Long docId, int index, String ext, byte[] bytes) throws IOException {
        Path imgDir = resolveDocDir(userId, docId).resolve("images");
        Files.createDirectories(imgDir);
        String safeExt = (ext == null || ext.trim().isEmpty()) ? "bin" : ext.trim().toLowerCase();
        Path dest = imgDir.resolve(index + "." + safeExt);
        Files.write(dest, bytes);
    }

    public Path findDocImage(Long userId, Long docId, int index) throws IOException {
        Path imgDir = resolveDocDir(userId, docId).resolve("images");
        if (!Files.isDirectory(imgDir)) {
            return null;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(imgDir, index + ".*")) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    return p;
                }
            }
        }
        return null;
    }

    public void deleteIfExists(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }
        Path abs = toAbsolute(relativePath);
        try {
            Files.deleteIfExists(abs);
            Path docDir = abs.getParent();
            if (docDir != null) {
                try {
                    Files.deleteIfExists(docDir);
                } catch (IOException ignored) {
                    // 目录非空等，忽略
                }
            }
        } catch (IOException e) {
            log.warn("删除文档原件失败: {}", abs, e);
        }
    }

    public Path toAbsolute(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            throw new IllegalArgumentException("路径为空");
        }
        Path normalized = resolveRoot().resolve(relativePath).normalize();
        if (!normalized.startsWith(resolveRoot())) {
            throw new IllegalArgumentException("非法路径");
        }
        return normalized;
    }

    static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "file";
        }
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        if (base.length() > 200) {
            base = base.substring(0, 200);
        }
        return base.isEmpty() ? "file" : base;
    }
}
