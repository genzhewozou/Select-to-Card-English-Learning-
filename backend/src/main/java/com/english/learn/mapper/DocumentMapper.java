package com.english.learn.mapper;

import com.english.learn.dto.DocumentDTO;
import com.english.learn.dto.DocumentSummaryDTO;
import com.english.learn.entity.Document;
import org.springframework.beans.BeanUtils;

/**
 * 文档 Entity 与 DTO 转换。
 */
public final class DocumentMapper {

    private DocumentMapper() {
    }

    public static Document toEntity(DocumentDTO dto) {
        if (dto == null) {
            return null;
        }
        Document entity = new Document();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    public static DocumentDTO toDTO(Document entity) {
        if (entity == null) {
            return null;
        }
        DocumentDTO dto = new DocumentDTO();
        BeanUtils.copyProperties(entity, dto);
        dto.setOriginalAvailable(
                entity.getStoredFilePath() != null && !entity.getStoredFilePath().isEmpty());
        return dto;
    }

    public static DocumentDTO fromSummary(DocumentSummaryDTO summary) {
        if (summary == null) {
            return null;
        }
        DocumentDTO dto = new DocumentDTO();
        dto.setId(summary.getId());
        dto.setUserId(summary.getUserId());
        dto.setFileName(summary.getFileName());
        dto.setFileType(summary.getFileType());
        dto.setGmtCreate(summary.getGmtCreate());
        dto.setGmtModified(summary.getGmtModified());
        dto.setOriginalAvailable(
                summary.getStoredFilePath() != null && !summary.getStoredFilePath().isEmpty());
        return dto;
    }
}
