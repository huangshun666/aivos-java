package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("`level`")
public class Level {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer threshold;   //门槛

    private String mark;        // 标识

    private BigDecimal award;      //奖励
}
