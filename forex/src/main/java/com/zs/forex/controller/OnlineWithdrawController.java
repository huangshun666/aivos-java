package com.zs.forex.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.common.pojo.OnlineWithdrawRecord;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.OnlineWithdrawRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnlineWithdrawController {

    @Autowired
    private OnlineWithdrawRecordService onlineWithdrawRecordService;

    @PostMapping("admin/withdraw/list")
    public ResultBody onlineWithdrawList(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = jsonObject.getInteger("pageIndex");
        Integer pageSize = jsonObject.getInteger("pageSize");
        Integer uid = jsonObject.getInteger("uid");

        Page<OnlineWithdrawRecord> page = onlineWithdrawRecordService.lambdaQuery()
                .eq(uid != null, OnlineWithdrawRecord::getUid, uid).page(new Page<>(pageIndex, pageSize));
        return ResultBody.success(page);

    }
}
