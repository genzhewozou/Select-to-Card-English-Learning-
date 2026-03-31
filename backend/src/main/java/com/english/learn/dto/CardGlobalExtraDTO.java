package com.english.learn.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CardGlobalExtraDTO {
    private Long id;
    private Long cardId;
    private List<String> collocations = new ArrayList<>();
    private String nativeTip;
    private String highLevelEn;
    private String highLevelZh;
}
