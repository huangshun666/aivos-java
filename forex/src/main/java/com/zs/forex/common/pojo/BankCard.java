package com.zs.forex.common.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class BankCard {

    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;

    private Integer uid;

    private Date ctime;

    private String bankName;           //银行名称

    private String bankAddress;        //银行地址

    private String currency;          //货币

    private String swift;

    private String nane;             //名称

    private String account;         //卡号

    private Integer type;          //0 商户 还是 1 客户
}
