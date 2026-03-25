package com.english.learn.controller;

import com.english.learn.common.PageResult;
import com.english.learn.common.Result;
import com.english.learn.dto.DocumentDTO;
import com.english.learn.dto.DocumentDownloadResult;
import com.english.learn.dto.DocumentImageResult;
import com.english.learn.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档 REST 接口：上传、列表、查看、删除。
 * 当前用户 ID 从请求头 X-User-Id 获取（实际项目建议用 JWT 或 Session）。
 */
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    private static Long getUserId(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        return userId;
    }

    /** POST /api/document/upload */
    @PostMapping("/upload")
    public Result<DocumentDTO> upload(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file) {
        return Result.success(documentService.upload(getUserId(userId), file));
    }

    /** GET /api/document/list */
    @GetMapping("/list")
    public Result<List<DocumentDTO>> list(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(documentService.listByUserId(getUserId(userId)));
    }

    /** GET /api/document/page - 服务端分页，默认 page=1,size=10 */
    @GetMapping("/page")
    public Result<PageResult<DocumentDTO>> page(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Page<DocumentDTO> p = documentService.pageByUserId(getUserId(userId), page, size);
        return Result.success(PageResult.of(page, size, p.getTotalElements(), p.getContent()));
    }

    /** GET /api/document/{id} */
    @GetMapping("/{id}")
    public Result<DocumentDTO> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(documentService.getById(id, getUserId(userId)));
    }

    /** GET /api/document/{id}/download — 下载服务器保存的上传原件（非 JSON） */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadOriginal(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        DocumentDownloadResult r = documentService.loadOriginalDownload(id, getUserId(userId));
        String encoded = UriUtils.encode(r.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(r.getResource());
    }

    /** GET /api/document/{id}/images/{index} — 读取文档中提取的图片 */
    @GetMapping("/{id}/images/{index}")
    public ResponseEntity<Resource> getDocImage(
            @PathVariable Long id,
            @PathVariable Integer index,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        DocumentImageResult r = documentService.loadDocImage(id, getUserId(userId), index);
        MediaType mt;
        try {
            mt = MediaType.parseMediaType(r.getContentType());
        } catch (Exception ignore) {
            mt = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(mt).body(r.getResource());
    }

    /** DELETE /api/document/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        documentService.deleteById(id, getUserId(userId));
        return Result.success();
    }
}
