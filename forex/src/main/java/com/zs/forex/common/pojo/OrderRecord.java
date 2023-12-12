package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName
public class OrderRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer uid;

    private String code;                                  //证券代码

    private Integer orderId;                              //关联单订单号

    private BigDecimal closePrice;                        //平仓价

    private BigDecimal openPrice;                          //开仓价

    private BigDecimal pl;                                //收益

    private BigDecimal plRate;                            //收益率

    private Date ctime;                                   //创建时间
}
