package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StrategyDTO {

    private String code;                        //证券名称

    private BigDecimal oldPrice;                //当前价

    private BigDecimal spread;                  //策略值

    private Integer decimal;                     //策略小数位

    private BigDecimal newPrice;                //调整后价格


}
