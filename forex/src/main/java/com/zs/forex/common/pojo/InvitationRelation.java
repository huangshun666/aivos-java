package com.zs.forex.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class InvitationRelation {
    @TableId(value = "id",type = IdType.AUTO)
    private Integer Id;

    private Integer uid;

    private Integer invitationId;

    private Date ctime;
}
