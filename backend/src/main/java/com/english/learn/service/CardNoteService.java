package com.english.learn.service;

import com.english.learn.dto.CardNoteDTO;
import com.english.learn.entity.Card;
import com.english.learn.entity.CardNote;
import com.english.learn.mapper.CardNoteMapper;
import com.english.learn.repository.CardRepository;
import com.english.learn.repository.CardNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 卡片注释业务服务：增删改查。保存时自动去掉内容中的连续空行。
 */
@Service
@RequiredArgsConstructor
public class CardNoteService {

    private static String normalizeEmptyLines(String s) {
        if (s == null || s.trim().isEmpty()) return s;
        return s.trim().replaceAll("\\n[\\s]*\\n", "\n");
    }

    private final CardNoteRepository cardNoteRepository;
    private final CardRepository cardRepository;

    @Transactional(rollbackFor = Exception.class)
    public CardNoteDTO create(CardNoteDTO dto, Long userId) {
        Card card = cardRepository.findById(dto.getCardId()).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限为该卡片添加注释");
        }
        CardNote entity = CardNoteMapper.toEntity(dto);
        if (entity.getContent() != null) {
            entity.setContent(normalizeEmptyLines(entity.getContent()));
        }
        entity = cardNoteRepository.save(entity);
        return CardNoteMapper.toDTO(entity);
    }

    public List<CardNoteDTO> listByCardId(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限查看");
        }
        return cardNoteRepository.findByCardId(cardId).stream().map(CardNoteMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public CardNoteDTO update(Long id, Long userId, CardNoteDTO dto) {
        CardNote entity = cardNoteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("注释不存在"));
        Card card = cardRepository.findById(entity.getCardId()).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改");
        }
        if (dto.getContent() != null) {
            entity.setContent(normalizeEmptyLines(dto.getContent()));
        }
        entity = cardNoteRepository.save(entity);
        return CardNoteMapper.toDTO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, Long userId) {
        CardNote note = cardNoteRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("注释不存在"));
        Card card = cardRepository.findById(note.getCardId()).orElseThrow(() -> new IllegalArgumentException("卡片不存在"));
        if (!card.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除");
        }
        cardNoteRepository.delete(note);
    }
}
