package com.english.learn.repository;

import com.english.learn.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    List<QuizSession> findTop30ByUserIdOrderByGmtCreateDesc(Long userId);
}
