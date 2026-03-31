package com.english.learn.repository;

import com.english.learn.entity.CardSense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CardSenseRepository extends JpaRepository<CardSense, Long> {

    List<CardSense> findByCardIdOrderBySortOrderAsc(Long cardId);

    List<CardSense> findByCardIdIn(Collection<Long> cardIds);

    void deleteByCardId(Long cardId);
}
