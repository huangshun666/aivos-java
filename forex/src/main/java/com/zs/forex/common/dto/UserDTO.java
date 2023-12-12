package com.zs.forex.common.dto;

import com.zs.forex.common.pojo.User;
import lombok.Data;

@Data
public class UserDTO {

    private Integer uid;

    private String phone;                   //手机号

    private String area;                    //区号

    private String email;                   //邮箱

    private Integer black;                  //是否为 黑名单   0 否 1 是

    private Integer auth;                   //是否进行实名认证  0 否 1 是

    private String relationCode;            //邀请码

    private String token;                   //token

    private  String  tradePwd;
}
