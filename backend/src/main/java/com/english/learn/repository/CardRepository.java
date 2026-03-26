package com.english.learn.repository;

import com.english.learn.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 卡片表数据访问层。
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserIdOrderByGmtCreateDesc(Long userId);

    List<Card> findByUserIdAndDocumentId(Long userId, Long documentId);

    long countByUserIdAndDocumentId(Long userId, Long documentId);

    Page<Card> findByUserIdOrderByGmtCreateDesc(Long userId, Pageable pageable);

    Page<Card> findByUserIdAndDocumentIdOrderByGmtCreateDesc(Long userId, Long documentId, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.userId = :userId AND (LOWER(c.frontContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.backContent) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY c.gmtCreate DESC")
    Page<Card> searchByUserId(Long userId, String keyword, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.userId = :userId AND c.documentId = :documentId AND (LOWER(c.frontContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.backContent) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY c.gmtCreate DESC")
    Page<Card> searchByUserIdAndDocumentId(Long userId, Long documentId, String keyword, Pageable pageable);

    /** 按 ID 批量查询（用于复习页避免 N+1） */
    List<Card> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    /** 从未复习过的卡片 ID（用于「今日待复习」快速补齐；可用 pageable 限制数量） */
    @Query(value = "SELECT c.id FROM learn_card c " +
            "LEFT JOIN learn_card_progress p ON p.card_id = c.id AND p.user_id = ?1 " +
            "WHERE c.user_id = ?1 AND p.id IS NULL " +
            "ORDER BY c.gmt_create DESC",
            nativeQuery = true)
    List<Long> findNeverReviewedCardIds(Long userId, Pageable pageable);

    interface CardRangeView {
        Long getId();
        Integer getStartOffset();
        Integer getEndOffset();
        String getFrontContent();
    }

    /** 文档内高亮用：只取范围信息，避免拉取 notes/progress 导致慢 */
    @Query(value = "SELECT c.id AS id, c.start_offset AS startOffset, c.end_offset AS endOffset, c.front_content AS frontContent " +
            "FROM learn_card c WHERE c.user_id = ?1 AND c.document_id = ?2",
            nativeQuery = true)
    List<CardRangeView> findRangesByUserIdAndDocumentId(Long userId, Long documentId);
}
