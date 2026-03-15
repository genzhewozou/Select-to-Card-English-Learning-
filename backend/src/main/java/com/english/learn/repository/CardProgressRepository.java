package com.english.learn.repository;

import com.english.learn.entity.CardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
}
