package com.english.learn.controller;

import com.english.learn.common.PageResult;
import com.english.learn.common.Result;
import com.english.learn.dto.CardDTO;
import com.english.learn.dto.CardProgressDTO;
import com.english.learn.dto.ReviewDocumentPostponeRequest;
import com.english.learn.dto.ReviewPostponeRequest;
import com.english.learn.dto.ReviewRequest;
import com.english.learn.service.CardProgressService;
import com.english.learn.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    public Result<List<CardDTO>> todayList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "documentId", required = false) Long documentId) {
        Long uid = getUserId(userId);
        List<Long> cardIds = new ArrayList<>(cardProgressService.findDueCardIds(uid, documentId));
        Collections.shuffle(cardIds, ThreadLocalRandom.current());
        List<CardDTO> cards = cardService.getByIdsInOrder(uid, cardIds);
        return Result.success(cards);
    }

    /** GET /api/review/today/page - 今日待复习（分页，默认 page=1,size=50） */
    @GetMapping("/today/page")
    public Result<PageResult<CardDTO>> todayPage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size) {
        Long uid = getUserId(userId);
        List<Long> allIds = cardProgressService.findDueCardIds(uid, documentId);
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 200));
        int from = Math.min((p - 1) * s, allIds.size());
        int to = Math.min(from + s, allIds.size());
        List<Long> pageIds = new ArrayList<>(allIds.subList(from, to));
        Collections.shuffle(pageIds, ThreadLocalRandom.current());
        List<CardDTO> cards = cardService.getByIdsInOrder(uid, pageIds);
        return Result.success(PageResult.of(p, s, allIds.size(), cards));
    }

    /** GET /api/review/weak - 错题本：熟练度 1-2 的卡片列表 */
    @GetMapping("/weak")
    public Result<List<CardDTO>> weakList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "documentId", required = false) Long documentId) {
        Long uid = getUserId(userId);
        return Result.success(cardService.listWithFilters(uid, documentId, null, 2, null));
    }

    /** GET /api/review/weak/page - 错题本分页（默认 page=1,size=10） */
    @GetMapping("/weak/page")
    public Result<PageResult<CardDTO>> weakPage(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Long uid = getUserId(userId);
        List<Long> allIds = cardProgressService.findWeakCardIds(uid, 2, documentId);
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 200));
        int from = Math.min((p - 1) * s, allIds.size());
        int to = Math.min(from + s, allIds.size());
        List<CardDTO> cards = cardService.getByIdsInOrder(uid, allIds.subList(from, to));
        return Result.success(PageResult.of(p, s, allIds.size(), cards));
    }

    /** POST /api/review/submit - 提交复习结果，更新熟练度与下次复习时间 */
    @PostMapping("/submit")
    public Result<CardProgressDTO> submit(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ReviewRequest request) {
        return Result.success(cardProgressService.submitReview(getUserId(userId), request));
    }

    /** POST /api/review/postpone - 延后复习时间（支持 1/2/7 天） */
    @PostMapping("/postpone")
    public Result<CardProgressDTO> postpone(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ReviewPostponeRequest request) {
        return Result.success(cardProgressService.postponeReview(getUserId(userId), request));
    }

    /** POST /api/review/postpone/document - 文档级延后（对文档下全部卡片生效） */
    @PostMapping("/postpone/document")
    public Result<Integer> postponeByDocument(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ReviewDocumentPostponeRequest request) {
        return Result.success(cardProgressService.postponeByDocument(getUserId(userId), request));
    }
}
