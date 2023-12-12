package com.zs.self.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ModelDTO {

    private Long time;

    private BigDecimal open;

    private BigDecimal low;

    private BigDecimal high;

    private BigDecimal close;

    private Integer refId;
}
