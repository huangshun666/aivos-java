package com.zs.forex.common.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PushDTO {
    private QuoteDTO quoteDTO;

    private Map<String,AggregatesDTO> aggregatesDTOMap;
}
