package com.english.learn.repository;

import com.english.learn.entity.CardNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 卡片注释表数据访问层。
 */
@Repository
public interface CardNoteRepository extends JpaRepository<CardNote, Long> {

    List<CardNote> findByCardId(Long cardId);

    Optional<CardNote> findFirstByCardId(Long cardId);
}
