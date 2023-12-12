package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class LoginRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer uid;

    private String ip;                      //ip

    private String device;                  //设备

    private String deviceNo;                //设备号

    private Date ctime;
}
