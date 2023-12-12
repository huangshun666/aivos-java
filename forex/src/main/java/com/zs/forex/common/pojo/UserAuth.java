package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class UserAuth {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String kycFacade;                               //正面

    private String kycObverse;                              //反面

    private String name;                                    //名称

    private String kycCode;                                 //认证号码

    private Integer status;                                 //是否审核状态 0 待审核  1  通过 2 不通过

    private String remark;                                  //备注

    private Integer uid;                                    //用户id

    private Date ctime;                                     //创建时间
}
