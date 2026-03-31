package com.english.learn.service;

import com.english.learn.dto.CardProgressDTO;
import com.english.learn.dto.ReviewDocumentPostponeRequest;
import com.english.learn.dto.ReviewPostponeRequest;
import com.english.learn.dto.ReviewRequest;
import com.english.learn.entity.Card;
import com.english.learn.entity.CardProgress;
import com.english.learn.mapper.CardProgressMapper;
import com.english.learn.repository.CardSourceRepository;
import com.english.learn.repository.CardProgressRepository;
import com.english.learn.repository.CardRepository;
import com.english.learn.util.EbbinghausUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final CardSourceRepository cardSourceRepository;

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
        // 2. 从未复习的卡片：没有进度记录的视为「今日待复习」
        Set<Long> dueCardIds = due.stream().map(CardProgressDTO::getCardId).collect(Collectors.toSet());
        // 为避免卡片数量很大时全量扫描，限制补齐数量（默认最多 200 张未复习卡片）
        List<Long> neverReviewed = cardRepository.findNeverReviewedCardIds(userId, PageRequest.of(0, 200));
        for (Long cardId : neverReviewed) {
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

    /** 今日及之前到期的卡片 ID 列表（可按文档过滤） */
    public List<Long> findDueCardIds(Long userId, Long documentId) {
        List<Long> dueIds = findDueCardIds(userId);
        if (documentId == null || dueIds.isEmpty()) {
            return dueIds;
        }
        Set<Long> idsInDocument = cardSourceRepository.findDistinctCardIdsByUserIdAndDocumentId(userId, documentId).stream()
                .collect(Collectors.toCollection(HashSet::new));
        return dueIds.stream().filter(idsInDocument::contains).collect(Collectors.toList());
    }

    /** 熟练度 <= maxLevel 的卡片 ID 列表（如 maxLevel=2 表示错题本） */
    public List<Long> findCardIdsByProficiencyMax(Long userId, int maxLevel) {
        return cardProgressRepository.findByUserIdAndProficiencyLevelLessThanEqual(userId, maxLevel).stream()
                .map(CardProgress::getCardId)
                .collect(Collectors.toList());
    }

    /** 错题本分页：熟练度 <= maxLevel 的 cardId 分页 */
    public Page<Long> pageWeakCardIds(Long userId, int maxLevel, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 100));
        PageRequest pr = PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "gmtModified"));
        return cardProgressRepository.findWeakCardIds(userId, maxLevel, pr);
    }

    /** 错题卡片 ID 列表（可按文档过滤） */
    public List<Long> findWeakCardIds(Long userId, int maxLevel, Long documentId) {
        List<Long> weakIds = cardProgressRepository.findByUserIdAndProficiencyLevelLessThanEqual(userId, maxLevel).stream()
                .map(CardProgress::getCardId)
                .collect(Collectors.toList());
        if (documentId == null || weakIds.isEmpty()) {
            return weakIds;
        }
        Set<Long> idsInDocument = cardSourceRepository.findDistinctCardIdsByUserIdAndDocumentId(userId, documentId).stream()
                .collect(Collectors.toCollection(HashSet::new));
        return weakIds.stream().filter(idsInDocument::contains).collect(Collectors.toList());
    }

    /** 延后复习：将下次复习时间顺延指定天数。 */
    @Transactional(rollbackFor = Exception.class)
    public CardProgressDTO postponeReview(Long userId, ReviewPostponeRequest request) {
        Card card = cardRepository.findById(request.getCardId()).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限");
        }
        int days = validatePostponeDays(request.getDays());
        CardProgress progress = postponeCard(userId, request.getCardId(), days);
        return CardProgressMapper.toDTO(progress);
    }

    /** 文档级延后：对文档下全部卡片统一顺延 1/2/7 天，返回影响卡片数。 */
    @Transactional(rollbackFor = Exception.class)
    public int postponeByDocument(Long userId, ReviewDocumentPostponeRequest request) {
        int days = validatePostponeDays(request.getDays());
        long totalCards = cardRepository.countByUserIdAndDocumentId(userId, request.getDocumentId());
        if (totalCards <= 0) {
            return 0;
        }
        // 1) 先补齐没有进度的卡片；2) 再一条 SQL 批量更新 next_review_at
        cardProgressRepository.insertMissingProgressByDocument(userId, request.getDocumentId());
        LocalDateTime nextReviewAt = LocalDateTime.now().plusDays(days);
        cardProgressRepository.bulkUpdateNextReviewAtByDocument(userId, request.getDocumentId(), nextReviewAt);
        return (int) totalCards;
    }

    private int validatePostponeDays(Integer days) {
        int d = days == null ? 0 : days;
        if (d != 1 && d != 2 && d != 7) {
            throw new IllegalArgumentException("仅支持延后 1/2/7 天");
        }
        return d;
    }

    private CardProgress postponeCard(Long userId, Long cardId, int days) {
        CardProgress progress = cardProgressRepository.findByUserIdAndCardId(userId, cardId)
                .orElseGet(() -> {
                    CardProgress p = new CardProgress();
                    p.setUserId(userId);
                    p.setCardId(cardId);
                    p.setReviewCount(0);
                    return p;
                });
        // 业务约定：延后应基于「当前操作时间」重算，而非在历史 nextReviewAt 上累加
        progress.setNextReviewAt(LocalDateTime.now().plusDays(days));
        return cardProgressRepository.save(progress);
    }
}
