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

    @Query(value = "SELECT c.* FROM learn_card c " +
            "WHERE c.user_id = :userId AND EXISTS (" +
            "SELECT 1 FROM learn_card_source cs WHERE cs.user_id = :userId AND cs.document_id = :documentId AND cs.card_id = c.id" +
            ") ORDER BY c.gmt_create DESC",
            nativeQuery = true)
    List<Card> findByUserIdAndDocumentId(Long userId, Long documentId);

    @Query(value = "SELECT COUNT(DISTINCT c.id) FROM learn_card c " +
            "WHERE c.user_id = :userId AND EXISTS (" +
            "SELECT 1 FROM learn_card_source cs WHERE cs.user_id = :userId AND cs.document_id = :documentId AND cs.card_id = c.id" +
            ")",
            nativeQuery = true)
    long countByUserIdAndDocumentId(Long userId, Long documentId);

    Page<Card> findByUserIdOrderByGmtCreateDesc(Long userId, Pageable pageable);

    @Query(value = "SELECT c.* FROM learn_card c " +
            "WHERE c.user_id = :userId AND EXISTS (" +
            "SELECT 1 FROM learn_card_source cs WHERE cs.user_id = :userId AND cs.document_id = :documentId AND cs.card_id = c.id" +
            ") ORDER BY c.gmt_create DESC",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM learn_card c " +
                    "WHERE c.user_id = :userId AND EXISTS (" +
                    "SELECT 1 FROM learn_card_source cs WHERE cs.user_id = :userId AND cs.document_id = :documentId AND cs.card_id = c.id" +
                    ")",
            nativeQuery = true)
    Page<Card> findByUserIdAndDocumentIdOrderByGmtCreateDesc(Long userId, Long documentId, Pageable pageable);

    @Query(
            value = "SELECT c.* FROM learn_card c " +
                    "WHERE c.user_id = :userId AND (" +
                    "LOWER(COALESCE(c.front_content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(s.translation_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.explanation_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.label, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.tone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_example e ON e.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(e.sentence_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.sentence_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.scenario_tag, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_synonym y ON y.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(y.lemma, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(y.note_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_global_extra g WHERE g.card_id = c.id AND (" +
                    "LOWER(COALESCE(g.native_tip, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.collocations_json, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    "))" +
                    ") ORDER BY c.gmt_create DESC",
            countQuery = "SELECT COUNT(*) FROM learn_card c " +
                    "WHERE c.user_id = :userId AND (" +
                    "LOWER(COALESCE(c.front_content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(s.translation_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.explanation_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.label, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.tone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_example e ON e.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(e.sentence_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.sentence_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.scenario_tag, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_synonym y ON y.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(y.lemma, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(y.note_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_global_extra g WHERE g.card_id = c.id AND (" +
                    "LOWER(COALESCE(g.native_tip, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.collocations_json, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    "))" +
                    ")",
            nativeQuery = true
    )
    Page<Card> searchByUserId(Long userId, String keyword, Pageable pageable);

    @Query(
            value = "SELECT c.* FROM learn_card c " +
                    "WHERE c.user_id = :userId AND EXISTS (" +
                    "SELECT 1 FROM learn_card_source cs WHERE cs.user_id = :userId AND cs.document_id = :documentId AND cs.card_id = c.id" +
                    ") AND (" +
                    "LOWER(COALESCE(c.front_content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(s.translation_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.explanation_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.label, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.tone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_example e ON e.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(e.sentence_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.sentence_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.scenario_tag, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_synonym y ON y.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(y.lemma, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(y.note_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_global_extra g WHERE g.card_id = c.id AND (" +
                    "LOWER(COALESCE(g.native_tip, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.collocations_json, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    "))" +
                    ") ORDER BY c.gmt_create DESC",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM learn_card c " +
                    "WHERE c.user_id = :userId AND EXISTS (" +
                    "SELECT 1 FROM learn_card_source cs WHERE cs.user_id = :userId AND cs.document_id = :documentId AND cs.card_id = c.id" +
                    ") AND (" +
                    "LOWER(COALESCE(c.front_content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(s.translation_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.explanation_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.label, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(s.tone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_example e ON e.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(e.sentence_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.sentence_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(e.scenario_tag, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_sense s JOIN learn_card_synonym y ON y.sense_id = s.id " +
                    "WHERE s.card_id = c.id AND (" +
                    "LOWER(COALESCE(y.lemma, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(y.note_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    ")) " +
                    "OR EXISTS (SELECT 1 FROM learn_card_global_extra g WHERE g.card_id = c.id AND (" +
                    "LOWER(COALESCE(g.native_tip, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_en, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.high_level_zh, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "OR LOWER(COALESCE(g.collocations_json, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                    "))" +
                    ")",
            nativeQuery = true
    )
    Page<Card> searchByUserIdAndDocumentId(Long userId, Long documentId, String keyword, Pageable pageable);

    /** 按 ID 批量查询（用于复习页避免 N+1） */
    List<Card> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    @Query(value = "SELECT c.* FROM learn_card c WHERE c.user_id = :userId AND LOWER(TRIM(c.front_content)) = LOWER(TRIM(:frontContent)) LIMIT 1", nativeQuery = true)
    Card findFirstByUserIdAndFrontContentNormalized(Long userId, String frontContent);

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
    @Query(value = "SELECT cs.card_id AS id, cs.start_offset AS startOffset, cs.end_offset AS endOffset, c.front_content AS frontContent " +
            "FROM learn_card_source cs JOIN learn_card c ON c.id = cs.card_id " +
            "WHERE cs.user_id = ?1 AND cs.document_id = ?2",
            nativeQuery = true)
    List<CardRangeView> findRangesByUserIdAndDocumentId(Long userId, Long documentId);
}
