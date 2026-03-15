package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.DocumentDTO;
import com.english.learn.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /** GET /api/document/{id} */
    @GetMapping("/{id}")
    public Result<DocumentDTO> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(documentService.getById(id, getUserId(userId)));
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
