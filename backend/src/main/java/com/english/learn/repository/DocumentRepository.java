package com.english.learn.repository;

import com.english.learn.dto.DocumentSummaryDTO;
import com.english.learn.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档表数据访问层。
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByGmtCreateDesc(Long userId);

    Page<Document> findByUserIdOrderByGmtCreateDesc(Long userId, Pageable pageable);

    /** 列表用：不加载 content（LONGTEXT），减轻数据库与网络压力 */
    @Query("SELECT new com.english.learn.dto.DocumentSummaryDTO(d.id, d.userId, d.fileName, d.fileType, d.gmtCreate, d.gmtModified, d.storedFilePath) "
            + "FROM Document d WHERE d.userId = :userId ORDER BY d.gmtCreate DESC, d.id DESC")
    List<DocumentSummaryDTO> findSummaryByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT new com.english.learn.dto.DocumentSummaryDTO(d.id, d.userId, d.fileName, d.fileType, d.gmtCreate, d.gmtModified, d.storedFilePath) "
            + "FROM Document d WHERE d.userId = :userId ORDER BY d.gmtCreate DESC, d.id DESC",
            countQuery = "SELECT count(d) FROM Document d WHERE d.userId = :userId")
    Page<DocumentSummaryDTO> findSummaryByUserId(@Param("userId") Long userId, Pageable pageable);
}
