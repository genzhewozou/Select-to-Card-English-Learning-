package com.english.learn.repository;

import com.english.learn.entity.CardSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CardSynonymRepository extends JpaRepository<CardSynonym, Long> {

    List<CardSynonym> findBySenseIdOrderBySortOrderAsc(Long senseId);

    List<CardSynonym> findBySenseIdIn(Collection<Long> senseIds);

    void deleteBySenseIdIn(Collection<Long> senseIds);
}
