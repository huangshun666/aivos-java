package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String pwd;                     //密码

    private String phone;                   //手机号

    private String name;                    //用户

    private String area;                    //区号

    private String email;                   //邮箱

    private Integer black;                  //是否为 黑名单   0 否 1 是

    private Integer auth;                   //是否进行实名认证  0 否 1 是

    private Integer online;                 //是否上封号  0 否  1 是

    private String relation;                //邀请关系

    private String relationCode;            //邀请码

    private Date ctime;                     //创建时间

    private Date mtime;                     //最近修改时间

    private String tradePwd;                //交易密码

}
