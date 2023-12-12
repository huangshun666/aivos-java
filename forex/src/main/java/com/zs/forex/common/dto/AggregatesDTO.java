package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class AggregatesDTO {

    private long t;             //时间

    private BigDecimal c;       //收盘价

    private BigDecimal h;       //最高价

    private BigDecimal l;       //最低价

    private BigDecimal o;       //开盘价

    private BigDecimal v;       //交易量




}
