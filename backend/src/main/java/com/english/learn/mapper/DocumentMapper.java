package com.english.learn.mapper;

import com.english.learn.dto.DocumentDTO;
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
        return dto;
    }
}
