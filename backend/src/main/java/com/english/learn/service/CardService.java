package com.english.learn.service;

import com.english.learn.dto.CardDTO;
import com.english.learn.dto.CardNoteDTO;
import com.english.learn.dto.CardProgressDTO;
import com.english.learn.entity.Card;
import com.english.learn.entity.CardNote;
import com.english.learn.entity.CardProgress;
import com.english.learn.mapper.CardMapper;
import com.english.learn.mapper.CardNoteMapper;
import com.english.learn.mapper.CardProgressMapper;
import com.english.learn.repository.CardRepository;
import com.english.learn.repository.CardNoteRepository;
import com.english.learn.repository.CardProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 卡片业务服务：CRUD、关联注释与进度；创建时可选 AI 生成注释。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final CardNoteRepository cardNoteRepository;
    private final CardProgressRepository cardProgressRepository;
    private final CardProgressService cardProgressService;
    private final AiNoteService aiNoteService;

    @Transactional(rollbackFor = Exception.class)
    public CardDTO create(CardDTO dto) {
        Card entity = CardMapper.toEntity(dto);
        entity = cardRepository.save(entity);
        // 新卡片立即进入「今日待复习」：创建一条进度记录，下次复习时间设为当前时间
        CardProgress progress = new CardProgress();
        progress.setUserId(entity.getUserId());
        progress.setCardId(entity.getId());
        progress.setReviewCount(0);
        progress.setNextReviewAt(LocalDateTime.now());
        cardProgressRepository.save(progress);
        // 注释来源：优先使用前端已生成好的 aiNoteContent，否则再根据 useAiNote+aiApiKey 调 AI
        String noteToSave = null;
        if (dto.getAiNoteContent() != null && !dto.getAiNoteContent().isBlank()) {
            noteToSave = dto.getAiNoteContent().trim();
        } else {
            boolean useAi = Boolean.TRUE.equals(dto.getUseAiNote());
            boolean hasKey = dto.getAiApiKey() != null && !dto.getAiApiKey().isBlank();
            if (useAi && hasKey) {
                Optional<String> aiNote = aiNoteService.generateNoteWithConfig(
                        entity.getFrontContent(), dto.getContextSentence(),
                        dto.getAiApiKey(), dto.getAiModel(), dto.getAiBaseUrl());
                noteToSave = aiNote.orElse(null);
                log.debug("AI note requested for card {}: {}", entity.getId(), noteToSave != null ? "generated" : "empty");
            } else if (useAi && !hasKey) {
                log.warn("Create card: useAiNote=true but aiApiKey missing or blank, skip AI");
            }
        }
        final long cardId = entity.getId();
        if (noteToSave != null) {
            try {
                CardNote note = new CardNote();
                note.setCardId(cardId);
                note.setContent(noteToSave);
                cardNoteRepository.save(note);
            } catch (Exception e) {
                log.warn("Save AI note failed for card {}: {}", cardId, e.getMessage());
            }
        }
        return fillAssociations(CardMapper.toDTO(entity, false), entity);
    }

    public List<CardDTO> listByUserId(Long userId) {
        return cardRepository.findByUserIdOrderByGmtCreateDesc(userId).stream()
                .map(e -> fillAssociations(CardMapper.toDTO(e, false), e))
                .collect(Collectors.toList());
    }

    public List<CardDTO> listByDocumentId(Long userId, Long documentId) {
        return cardRepository.findByUserIdAndDocumentId(userId, documentId).stream()
                .map(e -> fillAssociations(CardMapper.toDTO(e, false), e))
                .collect(Collectors.toList());
    }

    /**
     * 卡片列表（支持按文档、关键词、熟练度、今日待复习筛选）。
     * 保持与 listByUserId / listByDocumentId 兼容：不传筛选条件时行为一致。
     */
    public List<CardDTO> listWithFilters(Long userId, Long documentId, String keyword,
                                         Integer proficiencyMax, Boolean dueToday) {
        List<CardDTO> list = documentId != null
                ? listByDocumentId(userId, documentId)
                : listByUserId(userId);
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim().toLowerCase();
            list = list.stream().filter(c ->
                    (c.getFrontContent() != null && c.getFrontContent().toLowerCase().contains(k))
                            || (c.getBackContent() != null && c.getBackContent().toLowerCase().contains(k)))
                    .collect(Collectors.toList());
        }
        if (proficiencyMax != null) {
            Set<Long> weakIds = cardProgressService.findCardIdsByProficiencyMax(userId, proficiencyMax).stream()
                    .collect(Collectors.toSet());
            list = list.stream().filter(c -> c.getId() != null && weakIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }
        if (Boolean.TRUE.equals(dueToday)) {
            Set<Long> dueIds = cardProgressService.findDueCardIds(userId).stream().collect(Collectors.toSet());
            list = list.stream().filter(c -> c.getId() != null && dueIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public CardDTO getById(Long id, Long userId) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限查看该卡片");
        }
        return fillAssociations(CardMapper.toDTO(card, false), card);
    }

    @Transactional(rollbackFor = Exception.class)
    public CardDTO update(Long id, Long userId, CardDTO dto) {
        Card entity = cardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改该卡片");
        }
        if (dto.getFrontContent() != null) {
            entity.setFrontContent(dto.getFrontContent());
        }
        if (dto.getBackContent() != null) {
            entity.setBackContent(dto.getBackContent());
        }
        if (dto.getStartOffset() != null) {
            entity.setStartOffset(dto.getStartOffset());
        }
        if (dto.getEndOffset() != null) {
            entity.setEndOffset(dto.getEndOffset());
        }
        entity = cardRepository.save(entity);
        return fillAssociations(CardMapper.toDTO(entity, false), entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, Long userId) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除该卡片");
        }
        cardNoteRepository.findByCardId(id).forEach(cardNoteRepository::delete);
        cardProgressRepository.findByUserIdAndCardId(userId, id).ifPresent(cardProgressRepository::delete);
        cardRepository.delete(card);
    }

    private CardDTO fillAssociations(CardDTO dto, Card entity) {
        dto.setNotes(cardNoteRepository.findByCardId(entity.getId()).stream()
                .map(CardNoteMapper::toDTO).collect(Collectors.toList()));
        cardProgressRepository.findByUserIdAndCardId(entity.getUserId(), entity.getId())
                .ifPresent(p -> dto.setProgress(CardProgressMapper.toDTO(p)));
        return dto;
    }
}
