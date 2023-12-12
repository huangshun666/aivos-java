package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SummaryProxy {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer uid;        //  用户id

    private String proxyCode;   // 用户邀请码

    private Integer pevProxyId;  //  推荐人id

    private Integer level;       //代理等级

    @TableField("`group`")
    private Integer group;       //整个团队有效人数

    private BigDecimal deposit;   //本周总充值

    private BigDecimal withdraw;  //本周总提现

    private BigDecimal amount;    // 账户余额

    private BigDecimal nextPl;    //下级直推总收益

    private BigDecimal actualDistribution;  //本周实际发放

    private BigDecimal nextPlWeek;  //下级直推总收益本周

    private Date startTime;         //开始时间

    private Date endTime;         //结束时间

    private Integer settlement;   //是否结算
}
