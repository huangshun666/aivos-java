package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName
public class Account {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private BigDecimal balance;                                 //余额

    private BigDecimal unbalance;                               //冻结余额

    private String currency;                                    //货币

    private Date ctime;                                         //创建时间

    private Integer uid;                                         //用户ID



}
