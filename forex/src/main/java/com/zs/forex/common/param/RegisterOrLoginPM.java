package com.zs.forex.common.param;

import com.zs.forex.common.vcenum.CodeScenes;
import lombok.Data;

@Data
public class RegisterOrLoginPM {

    private String pwd;                     //密码

    private String phone;                   //手机号

    private String area;                    //区号

    private String email;                   //邮箱

    private String name;                   //姓名

    private String verifyCode;              //验证码

    private String relationCode;            //邀请码

    private CodeScenes scenes;              //场景

}
