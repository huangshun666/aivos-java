package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProxyInfoDTO {

    private String account;   //账号

    private Integer level;    //等级

    private Integer group;    //团队

    private BigDecimal deposit; //充值

    private BigDecimal pl;      //直推总收益
}
