package com.english.learn.mapper;

import com.english.learn.dto.CardDTO;
import com.english.learn.entity.Card;
import org.springframework.beans.BeanUtils;

import java.util.Collections;

/**
 * 卡片 Entity 与 DTO 转换（不含关联 notes/progress，由 Service 层填充）。
 */
public final class CardMapper {

    private CardMapper() {
    }

    public static Card toEntity(CardDTO dto) {
        if (dto == null) {
            return null;
        }
        Card entity = new Card();
        BeanUtils.copyProperties(dto, entity, "notes", "progress", "useAiNote", "aiApiKey", "aiModel", "aiBaseUrl", "aiNoteContent");
        return entity;
    }

    public static CardDTO toDTO(Card entity) {
        return toDTO(entity, false);
    }

    public static CardDTO toDTO(Card entity, boolean includeAssociations) {
        if (entity == null) {
            return null;
        }
        CardDTO dto = new CardDTO();
        BeanUtils.copyProperties(entity, dto, "notes", "progress");
        if (includeAssociations) {
            dto.setNotes(Collections.emptyList());
            dto.setProgress(null);
        }
        return dto;
    }
}
