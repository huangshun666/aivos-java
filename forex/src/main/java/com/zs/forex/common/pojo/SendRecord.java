package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class SendRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer type;                    //类型 0 短信 1 邮箱

    @TableField("`to`")
    private String to;                      //目标

    private String content;                //内容

    private Date ctime;                    //时间

}
