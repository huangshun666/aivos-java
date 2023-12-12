package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class AdminUser {

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private String userName;                                        //用户名

    private String password;                                        //用户密码

    private String googleCode;                                      //谷歌验证码

    private Integer type;                                           //用户身份

    private Integer createId;                                       //创建者id

    private Date ctime;                                             //创建时间


}
