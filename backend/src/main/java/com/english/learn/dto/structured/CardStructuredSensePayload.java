package com.english.learn.dto.structured;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class CardStructuredSensePayload {
    @NotNull
    private Integer order;
    private String label;
    private String translationZh;
    private String explanationEn;
    private String tone;
    @Valid
    private List<CardStructuredExamplePayload> examples = new ArrayList<>();
    @Valid
    private List<CardStructuredSynonymPayload> synonyms = new ArrayList<>();
}
