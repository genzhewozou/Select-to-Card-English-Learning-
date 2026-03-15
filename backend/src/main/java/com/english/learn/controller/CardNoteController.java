package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.CardNoteDTO;
import com.english.learn.service.CardNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 卡片注释 REST 接口。
 */
@RestController
@RequestMapping("/card/note")
@RequiredArgsConstructor
public class CardNoteController {

    private final CardNoteService cardNoteService;

    private static Long getUserId(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        return userId;
    }

    /** POST /api/card/note */
    @PostMapping
    public Result<CardNoteDTO> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CardNoteDTO dto) {
        return Result.success(cardNoteService.create(dto, getUserId(userId)));
    }

    /** GET /api/card/note/list?cardId=xxx */
    @GetMapping("/list")
    public Result<List<CardNoteDTO>> listByCardId(
            @RequestParam Long cardId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(cardNoteService.listByCardId(cardId, getUserId(userId)));
    }

    /** PUT /api/card/note/{id} */
    @PutMapping("/{id}")
    public Result<CardNoteDTO> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CardNoteDTO dto) {
        return Result.success(cardNoteService.update(id, getUserId(userId), dto));
    }

    /** DELETE /api/card/note/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        cardNoteService.deleteById(id, getUserId(userId));
        return Result.success();
    }
}
