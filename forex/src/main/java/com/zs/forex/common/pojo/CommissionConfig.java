package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommissionConfig {
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private BigDecimal efficientThreshold;  //有效会员门槛

    private BigDecimal tradeThreshold;      //下单门槛

    private BigDecimal commissionRate;
}
