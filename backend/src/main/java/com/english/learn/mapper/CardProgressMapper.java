package com.english.learn.mapper;

import com.english.learn.dto.CardProgressDTO;
import com.english.learn.entity.CardProgress;
import org.springframework.beans.BeanUtils;

/**
 * 学习进度 Entity 与 DTO 转换。
 */
public final class CardProgressMapper {

    private CardProgressMapper() {
    }

    public static CardProgress toEntity(CardProgressDTO dto) {
        if (dto == null) {
            return null;
        }
        CardProgress entity = new CardProgress();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    public static CardProgressDTO toDTO(CardProgress entity) {
        if (entity == null) {
            return null;
        }
        CardProgressDTO dto = new CardProgressDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
