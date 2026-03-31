package com.english.learn.dto.structured;

import lombok.Data;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
public class CardStructuredSaveRequest {
    @Valid
    private List<CardStructuredSensePayload> senses = new ArrayList<>();
    private CardStructuredGlobalPayload globalExtra;
}
