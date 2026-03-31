package com.english.learn.repository;

import com.english.learn.entity.QuizSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionItemRepository extends JpaRepository<QuizSessionItem, Long> {

    List<QuizSessionItem> findBySessionIdOrderBySequenceNoAsc(Long sessionId);
}
