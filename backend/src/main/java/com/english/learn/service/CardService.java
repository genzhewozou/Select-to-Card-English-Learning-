package com.english.learn.service;

import com.english.learn.dto.CardDTO;
import com.english.learn.dto.CardGlobalExtraDTO;
import com.english.learn.dto.CardProgressDTO;
import com.english.learn.dto.CardRangeDTO;
import com.english.learn.dto.CardSenseDTO;
import com.english.learn.entity.Card;
import com.english.learn.entity.CardProgress;
import com.english.learn.mapper.CardMapper;
import com.english.learn.mapper.CardProgressMapper;
import com.english.learn.repository.CardRepository;
import com.english.learn.repository.CardProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 卡片业务服务：CRUD、关联结构化释义与进度。
 * 保存时自动去掉背面内容中的空行（连续换行合并为单个换行）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    /** 去掉连续空行，合并为单个换行并 trim，便于粘贴内容后保存更整洁 */
    private static String normalizeEmptyLines(String s) {
        if (s == null || s.trim().isEmpty()) return s;
        return s.trim().replaceAll("\\n[\\s]*\\n", "\n");
    }

    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final CardProgressService cardProgressService;
    private final AiNoteService aiNoteService;
    private final CardStructuredContentService cardStructuredContentService;

    @Transactional(rollbackFor = Exception.class)
    public CardDTO create(CardDTO dto) {
        Card entity = CardMapper.toEntity(dto);
        if (entity.getBackContent() != null) {
            entity.setBackContent(normalizeEmptyLines(entity.getBackContent()));
        }
        entity = cardRepository.save(entity);
        // 新卡片立即进入「今日待复习」：创建一条进度记录，下次复习时间设为当前时间
        CardProgress progress = new CardProgress();
        progress.setUserId(entity.getUserId());
        progress.setCardId(entity.getId());
        progress.setReviewCount(0);
        progress.setNextReviewAt(LocalDateTime.now());
        cardProgressRepository.save(progress);

        // 创建阶段：若背面来自 AI（或请求要求后端生成），自动解析为结构化义项树并落库。
        String noteText = null;
        if (dto.getAiNoteContent() != null && !dto.getAiNoteContent().trim().isEmpty()) {
            noteText = normalizeEmptyLines(dto.getAiNoteContent());
        } else {
            boolean useAi = Boolean.TRUE.equals(dto.getUseAiNote());
            boolean hasKey = dto.getAiApiKey() != null && !dto.getAiApiKey().trim().isEmpty();
            if (useAi && hasKey) {
                Optional<String> aiNote = aiNoteService.generateNoteWithConfig(
                        entity.getFrontContent(), dto.getContextSentence(),
                        dto.getAiApiKey(), dto.getAiModel(), dto.getAiBaseUrl(), dto.getAiNotePrompt());
                noteText = aiNote.map(CardService::normalizeEmptyLines).orElse(null);
            }
        }
        if (noteText != null && !noteText.trim().isEmpty()) {
            // 若前端未填 backContent，则用 AI 内容补齐；随后尝试解析并覆盖为结构化汇总背面。
            if (entity.getBackContent() == null || entity.getBackContent().trim().isEmpty()) {
                entity.setBackContent(noteText);
                entity = cardRepository.save(entity);
            }
            try {
                cardStructuredContentService.tryApplyFromNoteText(entity.getId(), entity.getUserId(), noteText);
            } catch (Exception e) {
                log.warn("Parse AI note to structured failed for card {}: {}", entity.getId(), e.getMessage());
            }
        }
        return fillAssociations(CardMapper.toDTO(entity, false), entity);
    }

    public List<CardDTO> listByUserId(Long userId) {
        List<Card> cards = cardRepository.findByUserIdOrderByGmtCreateDesc(userId);
        return fillAssociationsBatch(userId, cards);
    }

    public List<CardDTO> listByDocumentId(Long userId, Long documentId) {
        List<Card> cards = cardRepository.findByUserIdAndDocumentId(userId, documentId);
        return fillAssociationsBatch(userId, cards);
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
        if (keyword != null && !keyword.trim().isEmpty()) {
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

    /** 服务端分页版列表（默认强制分页，避免一次拉全表导致慢/超时） */
    public Page<CardDTO> pageWithFilters(Long userId, Long documentId, String keyword,
                                         Integer proficiencyMax, Boolean dueToday,
                                         int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 100));
        PageRequest pr = PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "gmtCreate"));

        // 先分页查卡片（只查 card 表，关联后续批量填充）
        Page<Card> cardPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            cardPage = documentId != null
                    ? cardRepository.searchByUserIdAndDocumentId(userId, documentId, k, pr)
                    : cardRepository.searchByUserId(userId, k, pr);
        } else {
            cardPage = documentId != null
                    ? cardRepository.findByUserIdAndDocumentIdOrderByGmtCreateDesc(userId, documentId, pr)
                    : cardRepository.findByUserIdOrderByGmtCreateDesc(userId, pr);
        }

        List<CardDTO> dtos = fillAssociationsBatch(userId, cardPage.getContent());

        // 其余筛选（熟练度/今日待复习）在分页结果内继续过滤（通常量很小）；必要时可再下推到 DB
        if (proficiencyMax != null) {
            Set<Long> weakIds = cardProgressService.findCardIdsByProficiencyMax(userId, proficiencyMax).stream()
                    .collect(Collectors.toSet());
            dtos = dtos.stream().filter(c -> c.getId() != null && weakIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }
        if (Boolean.TRUE.equals(dueToday)) {
            Set<Long> dueIds = cardProgressService.findDueCardIds(userId).stream().collect(Collectors.toSet());
            dtos = dtos.stream().filter(c -> c.getId() != null && dueIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(dtos, pr, cardPage.getTotalElements());
    }

    public CardDTO getById(Long id, Long userId) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限查看该卡片");
        }
        return fillAssociations(CardMapper.toDTO(card, false), card);
    }

    public List<CardRangeDTO> listRangesByDocumentId(Long userId, Long documentId) {
        return cardRepository.findRangesByUserIdAndDocumentId(userId, documentId).stream().map(v -> {
            CardRangeDTO dto = new CardRangeDTO();
            dto.setId(v.getId());
            dto.setStartOffset(v.getStartOffset());
            dto.setEndOffset(v.getEndOffset());
            dto.setFrontContent(v.getFrontContent());
            return dto;
        }).collect(Collectors.toList());
    }

    /** 批量获取卡片详情（含 notes/progress），并按传入 id 顺序返回，用于复习页避免 N+1。 */
    public List<CardDTO> getByIdsInOrder(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return new ArrayList<>();
        List<Card> cards = cardRepository.findByUserIdAndIdIn(userId, cardIds);
        List<CardDTO> dtos = fillAssociationsBatch(userId, cards);
        Map<Long, CardDTO> byId = dtos.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(CardDTO::getId, d -> d, (a, b) -> a));
        List<CardDTO> ordered = new ArrayList<>(cardIds.size());
        for (Long id : cardIds) {
            CardDTO dto = byId.get(id);
            if (dto != null) ordered.add(dto);
        }
        return ordered;
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
            entity.setBackContent(normalizeEmptyLines(dto.getBackContent()));
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
        cardStructuredContentService.clearStructure(id);
        cardProgressRepository.findByUserIdAndCardId(userId, id).ifPresent(cardProgressRepository::delete);
        cardRepository.delete(card);
    }

    private CardDTO fillAssociations(CardDTO dto, Card entity) {
        dto.setSenses(cardStructuredContentService.loadSensesForCard(entity.getId()));
        dto.setGlobalExtra(cardStructuredContentService.loadGlobalForCard(entity.getId()));
        cardProgressRepository.findByUserIdAndCardId(entity.getUserId(), entity.getId())
                .ifPresent(p -> dto.setProgress(CardProgressMapper.toDTO(p)));
        return dto;
    }

    /**
     * 列表页批量填充关联，避免 N+1：
     * - 1 次查 card
     * - 1 次查结构化义项树（IN）
     * - 1 次查 card_progress（IN）
     */
    private List<CardDTO> fillAssociationsBatch(Long userId, List<Card> cards) {
        if (cards == null || cards.isEmpty()) return new ArrayList<>();

        List<Long> cardIds = cards.stream().map(Card::getId).collect(Collectors.toList());

        Map<Long, CardProgressDTO> progressByCardId = new HashMap<>();
        cardProgressRepository.findByUserIdAndCardIdIn(userId, cardIds).forEach(p -> {
            progressByCardId.put(p.getCardId(), CardProgressMapper.toDTO(p));
        });

        Map<Long, List<CardSenseDTO>> sensesByCard = cardStructuredContentService.loadSensesBatch(cardIds);
        Map<Long, CardGlobalExtraDTO> globalByCard = cardStructuredContentService.loadGlobalBatch(cardIds);

        List<CardDTO> list = new ArrayList<>(cards.size());
        for (Card c : cards) {
            CardDTO dto = CardMapper.toDTO(c, false);
            dto.setSenses(sensesByCard.getOrDefault(c.getId(), new ArrayList<>()));
            dto.setGlobalExtra(globalByCard.get(c.getId()));
            CardProgressDTO progress = progressByCardId.get(c.getId());
            if (progress != null) dto.setProgress(progress);
            list.add(dto);
        }
        return list;
    }
}
