package com.english.learn.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CardSenseDTO {
    private Long id;
    private Long cardId;
    private Integer sortOrder;
    private String label;
    private String translationZh;
    private String explanationEn;
    private String tone;
    private List<CardExampleDTO> examples = new ArrayList<>();
    private List<CardSynonymDTO> synonyms = new ArrayList<>();
}
