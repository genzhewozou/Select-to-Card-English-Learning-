package com.english.learn.repository;

import com.english.learn.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 卡片表数据访问层。
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserIdOrderByGmtCreateDesc(Long userId);

    List<Card> findByUserIdAndDocumentId(Long userId, Long documentId);
}
