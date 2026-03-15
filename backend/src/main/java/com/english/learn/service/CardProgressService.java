package com.english.learn.service;

import com.english.learn.dto.CardProgressDTO;
import com.english.learn.dto.ReviewRequest;
import com.english.learn.entity.Card;
import com.english.learn.entity.CardProgress;
import com.english.learn.mapper.CardProgressMapper;
import com.english.learn.repository.CardProgressRepository;
import com.english.learn.repository.CardRepository;
import com.english.learn.util.EbbinghausUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习进度与复习业务：艾宾浩斯下次复习时间、今日待复习列表。
 */
@Service
@RequiredArgsConstructor
public class CardProgressService {

    private final CardProgressRepository cardProgressRepository;
    private final CardRepository cardRepository;

    /**
     * 提交复习结果，更新熟练度与下次复习时间（艾宾浩斯）。
     */
    @Transactional(rollbackFor = Exception.class)
    public CardProgressDTO submitReview(Long userId, ReviewRequest request) {
        Card card = cardRepository.findById(request.getCardId()).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        CardProgress progress = cardProgressRepository.findByUserIdAndCardId(userId, request.getCardId())
                .orElseGet(() -> {
                    CardProgress p = new CardProgress();
                    p.setUserId(userId);
                    p.setCardId(request.getCardId());
                    p.setReviewCount(0);
                    return p;
                });
        progress.setProficiencyLevel(request.getProficiencyLevel());
        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setLastReviewAt(LocalDateTime.now());
        progress.setNextReviewAt(EbbinghausUtil.nextReviewAt(progress.getReviewCount(), request.getProficiencyLevel()));
        progress = cardProgressRepository.save(progress);
        return CardProgressMapper.toDTO(progress);
    }

    /**
     * 今日及之前到期的待复习卡片列表，包含两类：
     * 1. 已有进度且 next_review_at <= 当前时间 的卡片（到期即出现，避免时区/「当天」边界问题）；
     * 2. 从未复习过的卡片（无进度记录），使其首次即可在「今日复习」中出现。
     */
    public List<CardProgressDTO> findDueForReview(Long userId) {
        // 1. 到期进度：next_review_at <= 数据库 NOW()，避免 Java/MySQL 时区不一致
        List<CardProgressDTO> due = cardProgressRepository.findDueForReview(userId).stream()
                .map(CardProgressMapper::toDTO)
                .collect(Collectors.toList());
        // 2. 从未复习的卡片：该用户所有卡片中，没有进度记录的视为「今日待复习」
        Set<Long> dueCardIds = due.stream().map(CardProgressDTO::getCardId).collect(Collectors.toSet());
        Set<Long> hasProgressCardIds = new HashSet<>(cardProgressRepository.findCardIdsByUserId(userId));
        List<Long> allCardIds = cardRepository.findByUserIdOrderByGmtCreateDesc(userId).stream()
                .map(Card::getId)
                .collect(Collectors.toList());
        for (Long cardId : allCardIds) {
            if (hasProgressCardIds.contains(cardId)) continue;
            if (dueCardIds.contains(cardId)) continue;
            CardProgressDTO dto = new CardProgressDTO();
            dto.setCardId(cardId);
            dto.setReviewCount(0);
            due.add(dto);
            dueCardIds.add(cardId);
        }
        return due;
    }

    /** 今日及之前到期的卡片 ID 列表（供筛选「今日待复习」使用） */
    public List<Long> findDueCardIds(Long userId) {
        return findDueForReview(userId).stream()
                .map(CardProgressDTO::getCardId)
                .collect(Collectors.toList());
    }

    /** 熟练度 <= maxLevel 的卡片 ID 列表（如 maxLevel=2 表示错题本） */
    public List<Long> findCardIdsByProficiencyMax(Long userId, int maxLevel) {
        return cardProgressRepository.findByUserIdAndProficiencyLevelLessThanEqual(userId, maxLevel).stream()
                .map(CardProgress::getCardId)
                .collect(Collectors.toList());
    }
}
