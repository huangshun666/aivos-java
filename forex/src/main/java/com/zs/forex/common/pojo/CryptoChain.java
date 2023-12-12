package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CryptoChain {
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private Integer uid;        //用户id

    private String coin;        //币种

    private String chain;       //链

    private Integer type;       //0 商户 1客户

    private String address;     //地址

    private String remark;     //备注

    private BigDecimal maxNum;  //最大

    private BigDecimal minNum;  //最小

    private Date ctime;     //创建时间
}
