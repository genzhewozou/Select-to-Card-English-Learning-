package com.english.learn.repository;

import com.english.learn.entity.CardExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CardExampleRepository extends JpaRepository<CardExample, Long> {

    List<CardExample> findBySenseIdOrderBySortOrderAsc(Long senseId);

    List<CardExample> findBySenseIdIn(Collection<Long> senseIds);

    void deleteBySenseIdIn(Collection<Long> senseIds);
}
