package com.zs.forex.common.web;

import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.pojo.User;
import lombok.Builder;
import lombok.Data;

public class RequestBodyWeb {


    private static final ThreadLocal<RequestBodyDTO> currentReq = new ThreadLocal<>();

    public static void set(RequestBodyDTO dto) {
        currentReq.set(dto);
    }

    public static RequestBodyDTO get() {
        return currentReq.get();
    }

    @Builder
    @Data
    public static class RequestBodyDTO {

        private String ip;                      //ip

        private String lang;                    //语言

        private String device;                  //设备

        private String token;                   //认证码

        private String deviceNo;                //设备号

        private User user;                      //用户

        private AdminUser adminUser;           //后台用户
    }


}
