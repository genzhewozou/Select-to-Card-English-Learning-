package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.CardDTO;
import com.english.learn.dto.CardProgressDTO;
import com.english.learn.dto.ReviewRequest;
import com.english.learn.service.CardProgressService;
import com.english.learn.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 复习 REST 接口：今日待复习列表、提交复习结果。
 */
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final CardProgressService cardProgressService;
    private final CardService cardService;

    private static Long getUserId(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        return userId;
    }

    /** GET /api/review/today - 今日待复习的卡片列表（含卡片详情） */
    @GetMapping("/today")
    public Result<List<CardDTO>> todayList(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long uid = getUserId(userId);
        List<CardProgressDTO> progressList = cardProgressService.findDueForReview(uid);
        List<CardDTO> cards = progressList.stream()
                .map(p -> cardService.getById(p.getCardId(), uid))
                .collect(Collectors.toList());
        return Result.success(cards);
    }

    /** GET /api/review/weak - 错题本：熟练度 1-2 的卡片列表 */
    @GetMapping("/weak")
    public Result<List<CardDTO>> weakList(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long uid = getUserId(userId);
        return Result.success(cardService.listWithFilters(uid, null, null, 2, null));
    }

    /** POST /api/review/submit - 提交复习结果，更新熟练度与下次复习时间 */
    @PostMapping("/submit")
    public Result<CardProgressDTO> submit(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ReviewRequest request) {
        return Result.success(cardProgressService.submitReview(getUserId(userId), request));
    }
}
