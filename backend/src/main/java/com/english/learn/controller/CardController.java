package com.english.learn.controller;

import com.english.learn.common.PageResult;
import com.english.learn.common.Result;
import com.english.learn.dto.CardDTO;
import com.english.learn.dto.CardRangeDTO;
import com.english.learn.dto.structured.CardStructuredGenerateRequest;
import com.english.learn.dto.structured.CardStructuredSaveRequest;
import com.english.learn.service.CardService;
import com.english.learn.service.CardStructuredContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 卡片 REST 接口：创建、列表、详情、更新、删除。
 */
@RestController
@RequestMapping("/card")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardStructuredContentService cardStructuredContentService;

    private static Long getUserId(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        return userId;
    }

    /** POST /api/card */
    @PostMapping
    public Result<CardDTO> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CardDTO dto) {
        dto.setUserId(getUserId(userId));
        return Result.success(cardService.create(dto));
    }

    /** GET /api/card/list 支持 documentId、keyword、proficiencyMax、dueToday 筛选 */
    @GetMapping("/list")
    public Result<List<CardDTO>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "proficiencyMax", required = false) Integer proficiencyMax,
            @RequestParam(value = "dueToday", required = false) Boolean dueToday) {
        Long uid = getUserId(userId);
        return Result.success(cardService.listWithFilters(uid, documentId, keyword, proficiencyMax, dueToday));
    }

    /** GET /api/card/page - 服务端分页（默认 page=1,size=10），避免一次拉全表导致慢/超时 */
    @GetMapping("/page")
    public Result<PageResult<CardDTO>> page(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "proficiencyMax", required = false) Integer proficiencyMax,
            @RequestParam(value = "dueToday", required = false) Boolean dueToday,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Long uid = getUserId(userId);
        Page<CardDTO> p = cardService.pageWithFilters(uid, documentId, keyword, proficiencyMax, dueToday, page, size);
        return Result.success(PageResult.of(page, size, p.getTotalElements(), p.getContent()));
    }

    /** GET /api/card/ranges?documentId=xxx - 文档内高亮范围（轻量） */
    @GetMapping("/ranges")
    public Result<List<CardRangeDTO>> ranges(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("documentId") Long documentId) {
        Long uid = getUserId(userId);
        return Result.success(cardService.listRangesByDocumentId(uid, documentId));
    }

    /** GET /api/card/{id} */
    @GetMapping("/{id}")
    public Result<CardDTO> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(cardService.getById(id, getUserId(userId)));
    }

    /** PUT /api/card/{id} */
    @PutMapping("/{id}")
    public Result<CardDTO> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CardDTO dto) {
        return Result.success(cardService.update(id, getUserId(userId), dto));
    }

    /** DELETE /api/card/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        cardService.deleteById(id, getUserId(userId));
        return Result.success();
    }

    /** POST /api/card/{id}/structured/generate — 内置 JSON schema，AI 结构化释义并落库 */
    @PostMapping("/{id}/structured/generate")
    public Result<CardDTO> generateStructured(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CardStructuredGenerateRequest req) {
        Long uid = getUserId(userId);
        cardStructuredContentService.generateAndApply(id, uid, req);
        return Result.success(cardService.getById(id, uid));
    }

    /** PUT /api/card/{id}/structured — 手动保存义项树（全量覆盖） */
    @PutMapping("/{id}/structured")
    public Result<CardDTO> saveStructured(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CardStructuredSaveRequest req) {
        Long uid = getUserId(userId);
        cardStructuredContentService.replaceStructure(id, uid, req);
        return Result.success(cardService.getById(id, uid));
    }
}
