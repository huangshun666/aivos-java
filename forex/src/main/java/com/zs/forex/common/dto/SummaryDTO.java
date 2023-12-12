package com.zs.forex.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SummaryDTO {

    private BigDecimal actualTRA;  //实际充值总金额

    private BigDecimal giftTRA;  //充值赠送总金额

    private BigDecimal experienceTRA;  //赠送体验金总金额

    private BigDecimal yesterdayActualTRA;  //昨日实际充值总金额

    private BigDecimal yesterdayGiftTRA;  //昨日充值赠送总金额

    private BigDecimal yesterdayExperienceTRA;  //昨日赠送体验金总金额

    private BigDecimal dayActualTRA;  //今日实际充值总金额

    private BigDecimal dayGiftTRA;  //今日充值赠送总金额

    private BigDecimal dayExperienceTRA;  //今日赠送体验金总金额

    private BigDecimal withdrawTRA;    //用户提现总金额

    private BigDecimal withdrawDayTRA;    //今日用户提现总金额

    private BigDecimal withdrawYesterdayTRA;    //昨日用户提现总金额；

    private Integer userTotal;//总用户数

    private Integer userDayTotal;//今日新增用户数

    private Integer userYesterdayTotal;//昨日新增用户数；

    private Integer efficientUserTotal; //总有效用户数

    private Integer efficientUserDayTotal; //今日新增有效用户数

    private Integer efficientUserYesterdayTotal; //昨日新增有效用户数

    private BigDecimal turnoverTotal; //总流水

    private BigDecimal turnoverDayTotal; //今日总流水

    private BigDecimal turnoverYesterdayTotal; //昨日总流水

    private BigDecimal tradeTotal; //用户交易总收益

    private BigDecimal dayTradeTotal; //今日用户交易总收益

    private BigDecimal yesterdayTradeTotal; //昨日用户交易总收益
}
