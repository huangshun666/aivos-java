package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParseDTO {

    private BigDecimal price;

    private long time;

    private String symbol;
}
