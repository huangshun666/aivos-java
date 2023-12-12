package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RewardRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer Id;

    private Integer proxyId;        //哪个代理

    private Integer uid;            //充值用户

    private String urrId;          //充值订单关联id

    private BigDecimal money;       //奖励金额

    private Integer mark;           //哪个等级

    private Date ctime;             //奖励时间


}
