package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuoteDTO {

    private String p;             //最新价

    private long t;              //时间戳

    private BigDecimal c;        //收盘价

    private BigDecimal h;        //最高价

    private BigDecimal l;        //最低价

    private BigDecimal o;        //开盘价

    private BigDecimal r;        //涨跌幅

    private String pair;         //币对

    private BigDecimal pevc;     //昨收
}
