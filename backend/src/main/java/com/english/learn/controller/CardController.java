package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.CardDTO;
import com.english.learn.service.CardService;
import lombok.RequiredArgsConstructor;
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
}
