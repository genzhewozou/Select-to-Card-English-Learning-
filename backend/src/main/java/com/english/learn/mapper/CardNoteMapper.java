package com.english.learn.mapper;

import com.english.learn.dto.CardNoteDTO;
import com.english.learn.entity.CardNote;
import org.springframework.beans.BeanUtils;

/**
 * 卡片注释 Entity 与 DTO 转换。
 */
public final class CardNoteMapper {

    private CardNoteMapper() {
    }

    public static CardNote toEntity(CardNoteDTO dto) {
        if (dto == null) {
            return null;
        }
        CardNote entity = new CardNote();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    public static CardNoteDTO toDTO(CardNote entity) {
        if (entity == null) {
            return null;
        }
        CardNoteDTO dto = new CardNoteDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
