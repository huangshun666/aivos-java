package com.zs.self.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName
public class ModelTask {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String market;

    private Integer symbolType;

    private String symbol;

    private String spreadScope;      //点差范围

    private String amountScope;      //数量范围

    private String frequencyScope;   //频率范围

    private Date startTime;          //开始周期

    private Date endTime;           //结束周期

    private Integer type;           //1 正常  2 强制停止 3 过期

    private String uuid;            //每次停止的uuid 用于强制停止

    private Integer systemId;       //系统id
}
