package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CalculateDTO {

    private BigDecimal bond;

    private BigDecimal fee;

    private BigDecimal price;
}
