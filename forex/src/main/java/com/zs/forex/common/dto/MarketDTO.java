package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class MarketDTO {

    private Integer type;
    private String market;
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal chg;
    private BigDecimal chgV;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal prvClose;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal open;
    private Date time;


}
