package com.zs.forex.common.dto;

import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.vcenum.Cmd;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RequestDTO {

    private Cmd cmd;


    private Integer uid;

}

