package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName
public class UserAccountRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer uid;            //用户id

    private Integer aid;            //关联的户头

    private BigDecimal money;       //变动金额

    private Integer type;           //类型  0 交易 1 手续费 2 出金 3 入金 4 充值 5 撤单回退  6 贈送金  7  信用金 8 下分

    private BigDecimal after;       //之后的金额

    @TableField("`before`")
    private BigDecimal before;      //之前的金额

    private String refId;           //关联的id

    private String remark;          //备注

    private Date ctime;

}
