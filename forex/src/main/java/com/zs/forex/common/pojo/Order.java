package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Data
@TableName("`order`")
public class Order {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String serial;                                   //序列号

    private Integer uid;                                    //用户ID

    private String code;                                    //证券代码

    private BigDecimal hs;                                  //多少手

    private BigDecimal tp;                                 //止盈

    private BigDecimal sl;                                 //止损

    private Integer direction;                             //方向 0 买入 1 卖出

    private Integer lever;                                 //杠杆倍数

    private BigDecimal bond;                               //保证金

    private BigDecimal dealPrice;                          //成交价

    private Integer type;                                  //0 市价单  1 限价单

    private Integer status;                                //0 待成交 1 已成交  2 已撤单  3 已平仓

    private BigDecimal limitPrice;                         //挂单价

    private BigDecimal fee;                                //手续费

    private BigDecimal num;                                //总数量

    private String remark;                                 //备注

    @TableField("`lock`")
    private Integer lock;                                  //是否锁仓 0 否  1 是

    private Date ctime;

    private Date mtime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);

    }

    //重写hashCode详见Objects.hash()方法
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
