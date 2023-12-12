package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TickDTO {

    private Integer type;
    private String market;
    private String symbol;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal quantity;
    private long time;
}
