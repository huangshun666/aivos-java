package com.zs.forex.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ClearDTO {

    private Integer uid;                                       //用户id

    private BigDecimal num;                                  //数量

    private BigDecimal o;                                   //开仓价

    private Integer orderId;                                 //订单id

    private String code;                                     //证券代码

    private String base;                                     //基准货币

    private String quote;                                   //计价货币

    private BigDecimal pl;                                  //收益

    private BigDecimal plRate;                              //收益率

    private BigDecimal closePrice;                          //平仓价

    private BigDecimal bond;                               //保证金

    private Integer direction;                             //方向 0 买入 1 卖出


}
