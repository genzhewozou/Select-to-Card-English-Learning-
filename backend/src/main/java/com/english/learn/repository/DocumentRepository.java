package com.english.learn.repository;

import com.english.learn.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档表数据访问层。
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByGmtCreateDesc(Long userId);

    Page<Document> findByUserIdOrderByGmtCreateDesc(Long userId, Pageable pageable);
}
