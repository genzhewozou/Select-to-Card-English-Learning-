package com.english.learn.dto.structured;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CardStructuredGlobalPayload {
    private List<String> collocations = new ArrayList<>();
    private String nativeTip;
    private String highLevelEn;
    private String highLevelZh;
}
