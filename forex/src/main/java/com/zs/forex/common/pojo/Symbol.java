package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 证券实体
 */
@Data
@TableName
public class Symbol {


    @TableId(type = IdType.INPUT)
    private String code;                                               //市场标识

    private String base;                                               //基准货币

    private String quote;                                              //计价货币

    private Integer type;                                              //类型 0.外汇 1.贵金属 2.原油 3 指数 4 虚拟币

    private BigDecimal size;                                           //一张=多少量  默认 10000

    private BigDecimal hs;                                             //最小下单量 hold hand min size 缩写  默认 0.01

    private Integer distance;                                          //挂单距离 默认 50个点

    private String icon;                                                //图标

    private Integer lever;                                               //杠杆倍数

    private BigDecimal fr;                                              //强平率 force rate 缩写 逐仓时使用 默认 20%

    private Integer online;                                              //是否上线 0 上线 1 下线

    private BigDecimal fre;                                              //手续费率

    private Integer hasSelf;                                           //0不是 1是

    @TableField("`precision`")
    private Integer precision;                                           //小数位数


}
