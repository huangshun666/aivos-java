package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class Media {
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private Integer type;

    private String url;

    @TableField("`group`")
    private String group;

    private Date ctime;
}
