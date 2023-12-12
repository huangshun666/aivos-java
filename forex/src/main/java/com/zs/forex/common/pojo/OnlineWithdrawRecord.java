package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OnlineWithdrawRecord {
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private Integer uid;              //用户id

    private String orderId;          //订单id

    private String param;              //参数

    private Integer status;            //状态 0 正常完成执行 1 异常 2 已发放

    private Date execTime;             //执行时间

    private String result;             //借口执行结果

    private Date endTime;              //到账时间




}
