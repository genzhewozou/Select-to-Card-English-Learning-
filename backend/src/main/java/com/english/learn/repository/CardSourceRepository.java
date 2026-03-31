package com.english.learn.repository;

import com.english.learn.entity.CardSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface CardSourceRepository extends JpaRepository<CardSource, Long> {

    List<CardSource> findByUserIdAndDocumentId(Long userId, Long documentId);

    @Query(value = "SELECT DISTINCT cs.card_id FROM learn_card_source cs WHERE cs.user_id = ?1 AND cs.document_id = ?2", nativeQuery = true)
    List<Long> findDistinctCardIdsByUserIdAndDocumentId(Long userId, Long documentId);

    @Query(value = "SELECT COUNT(DISTINCT cs.card_id) FROM learn_card_source cs WHERE cs.user_id = ?1 AND cs.document_id = ?2", nativeQuery = true)
    long countDistinctCardIdsByUserIdAndDocumentId(Long userId, Long documentId);

    Optional<CardSource> findFirstByUserIdAndCardIdAndDocumentIdAndStartOffsetAndEndOffset(
            Long userId, Long cardId, Long documentId, Integer startOffset, Integer endOffset);

    void deleteByUserIdAndCardId(Long userId, Long cardId);

    void deleteByUserIdAndDocumentId(Long userId, Long documentId);

    List<CardSource> findByUserIdAndCardIdIn(Long userId, Collection<Long> cardIds);
}
