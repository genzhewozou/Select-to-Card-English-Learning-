package com.english.learn.repository;

import com.english.learn.entity.CardGlobalExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardGlobalExtraRepository extends JpaRepository<CardGlobalExtra, Long> {

    Optional<CardGlobalExtra> findByCardId(Long cardId);

    void deleteByCardId(Long cardId);

    List<CardGlobalExtra> findByCardIdIn(Collection<Long> cardIds);
}
