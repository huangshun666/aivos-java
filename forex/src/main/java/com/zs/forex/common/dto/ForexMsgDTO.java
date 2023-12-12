package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ForexMsgDTO {

    private String p;

    private BigDecimal a;

    private BigDecimal b;

    private Date t;
}
