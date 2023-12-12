package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LiquidationDTO {

    private Integer uid;

    private BigDecimal sumBond;     //总保证金

    private BigDecimal sumPl;       //总收益

    private BigDecimal price;
}
