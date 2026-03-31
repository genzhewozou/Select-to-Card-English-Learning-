package com.english.learn.repository;

import com.english.learn.entity.CardProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

/**
 * 学习进度表数据访问层。
 */
@Repository
public interface CardProgressRepository extends JpaRepository<CardProgress, Long> {

    Optional<CardProgress> findByUserIdAndCardId(Long userId, Long cardId);

    /** 某用户下所有有进度记录的 cardId（用于找出「从未复习」的卡片） */
    @Query("SELECT p.cardId FROM CardProgress p WHERE p.userId = :userId")
    List<Long> findCardIdsByUserId(Long userId);

    /**
     * 查询某用户已到期的卡片进度（next_review_at <= 数据库当前时间）。
     * 使用数据库 NOW() 避免 Java 与 MySQL 时区不一致导致「已过期却查不出」。
     */
    @Query(value = "SELECT * FROM learn_card_progress p WHERE p.user_id = ?1 AND p.next_review_at <= NOW() ORDER BY p.next_review_at ASC", nativeQuery = true)
    List<CardProgress> findDueForReview(Long userId);

    /** 熟练度 <= maxLevel 的进度（用于错题本） */
    List<CardProgress> findByUserIdAndProficiencyLevelLessThanEqual(Long userId, Integer maxLevel);

    /** 批量查询进度（用于列表页避免 N+1） */
    List<CardProgress> findByUserIdAndCardIdIn(Long userId, Collection<Long> cardIds);

    /** 错题本分页：直接在 progress 上分页，保证 total/分页准确 */
    @Query("SELECT p.cardId FROM CardProgress p WHERE p.userId = :userId AND p.proficiencyLevel <= :maxLevel ORDER BY p.gmtModified DESC")
    Page<Long> findWeakCardIds(Long userId, Integer maxLevel, Pageable pageable);

    /** 为文档下缺失进度记录的卡片批量补齐空进度（避免逐条 save）。 */
    @Modifying
    @Query(value = "INSERT INTO learn_card_progress (user_id, card_id, review_count, next_review_at, gmt_create, gmt_modified) " +
            "SELECT t.user_id, t.card_id, 0, NOW(), NOW(), NOW() " +
            "FROM (SELECT DISTINCT user_id, card_id FROM learn_card_source WHERE user_id = :userId AND document_id = :documentId) t " +
            "LEFT JOIN learn_card_progress p ON p.user_id = t.user_id AND p.card_id = t.card_id " +
            "WHERE p.id IS NULL",
            nativeQuery = true)
    int insertMissingProgressByDocument(@Param("userId") Long userId, @Param("documentId") Long documentId);

    /** 批量更新文档下全部卡片的 next_review_at。 */
    @Modifying
    @Query(value = "UPDATE learn_card_progress p " +
            "JOIN (SELECT DISTINCT user_id, card_id FROM learn_card_source WHERE user_id = :userId AND document_id = :documentId) t " +
            "ON t.user_id = p.user_id AND t.card_id = p.card_id " +
            "SET p.next_review_at = :nextReviewAt, p.gmt_modified = NOW() " +
            "WHERE p.user_id = :userId",
            nativeQuery = true)
    int bulkUpdateNextReviewAtByDocument(@Param("userId") Long userId,
                                         @Param("documentId") Long documentId,
                                         @Param("nextReviewAt") LocalDateTime nextReviewAt);
}
