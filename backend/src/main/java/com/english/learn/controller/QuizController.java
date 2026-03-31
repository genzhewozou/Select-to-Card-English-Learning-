package com.english.learn.controller;

import com.english.learn.common.Result;
import com.english.learn.dto.quiz.*;
import com.english.learn.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 文档测验：会话出题、作答、结果、错题再练。
 */
@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    private static Long getUserId(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        return userId;
    }

    @PostMapping("/session")
    public Result<QuizSessionStartResponse> start(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody QuizSessionStartRequest req) {
        return Result.success(quizService.startSession(getUserId(userId), req));
    }

    @PostMapping("/session/{sessionId}/answer")
    public Result<QuizAnswerResponse> answer(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody QuizAnswerRequest req) {
        return Result.success(quizService.submitAnswer(getUserId(userId), sessionId, req));
    }

    @GetMapping("/session/{sessionId}/result")
    public Result<QuizResultResponse> result(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(quizService.getResult(getUserId(userId), sessionId));
    }

    @GetMapping("/session/{sessionId}")
    public Result<QuizSessionStartResponse> detail(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(quizService.getSessionDetail(getUserId(userId), sessionId));
    }

    @GetMapping("/session/list")
    public Result<List<QuizResultResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(quizService.listRecentResults(getUserId(userId)));
    }

    @PostMapping("/session/{sessionId}/retry-wrong")
    public Result<QuizSessionStartResponse> retryWrong(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.success(quizService.retryWrong(getUserId(userId), sessionId));
    }
}
