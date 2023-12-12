package com.zs.self.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@TableName
@Data
public class Model {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Long time;

    private BigDecimal open;

    private BigDecimal low;

    private BigDecimal high;

    private BigDecimal close;

    private Integer refId;
}
