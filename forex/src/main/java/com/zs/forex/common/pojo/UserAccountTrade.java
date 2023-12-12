package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName
public class UserAccountTrade {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private Integer uid;                // 用户id

    private Integer bankCardId;         // 银行卡id

    private Integer type;               //类型 0 入金 1 出金

    private BigDecimal money;           //金额

    private Integer status;             //状态 0  审核中 1  已通过  2 未通过  3 已取消

    private String certificate;         //凭证图片

    private Integer tradeType;         // 渠道 0 银行卡 1 虚拟币

    private Date ctime;

    private Date mtime;

}
