package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class IndexMsgDTO {

    private BigDecimal val;

    private String T;

    private Date time;
}
