package com.zs.forex.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.aop.NeedLogin;
import com.zs.forex.common.pojo.CommissionConfig;
import com.zs.forex.common.pojo.Level;
import com.zs.forex.common.pojo.SummaryProxy;
import com.zs.forex.common.pojo.User;

import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.CommissionConfigService;
import com.zs.forex.service.LevelService;

import com.zs.forex.service.SummaryProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommissionController {

    @Autowired
    private CommissionConfigService commissionConfigService;

    @Autowired
    private SummaryProxyService summaryProxyService;


    @Autowired
    private LevelService levelService;

    @PostMapping("/commission/directPushDetails")
    @NeedLogin
    public ResultBody directPushDetails() {
        RequestBodyWeb.RequestBodyDTO requestBodyDTO = RequestBodyWeb.get();
        User user = requestBodyDTO.getUser();
        return ResultBody.success(levelService.directPushDetails(user.getId()));

    }

    @PostMapping("/commission/getLevel")
    @NeedLogin
    public ResultBody getLevel() {
        RequestBodyWeb.RequestBodyDTO requestBodyDTO = RequestBodyWeb.get();

        User user = requestBodyDTO.getUser();

        return ResultBody.success(levelService.getLevel(user.getId(), false));

    }


    @PostMapping("admin/commission/addOrUpdateLevel")
    @AdminNeedLogin
    public ResultBody addOrUpdateLevel(@RequestBody Level level) {
        return ResultBody.success(levelService.saveOrUpdate(level));
    }

    @PostMapping("admin/commission/delLevel")
    @AdminNeedLogin
    public ResultBody delLevel(@RequestBody Level level) {
        return ResultBody.success(levelService.removeById(level.getId()));
    }


    @PostMapping("admin/commission/getLevel")
    @AdminNeedLogin
    public ResultBody delLevel() {
        return ResultBody.success(levelService.lambdaQuery().orderByAsc(Level::getMark).list());
    }

    @PostMapping("admin/commission/addOrUpdateCommissionConfig")
    @AdminNeedLogin
    public ResultBody addOrUpdateCommissionConfig(@RequestBody CommissionConfig commissionConfig) {
        return ResultBody.success(commissionConfigService.saveOrUpdate(commissionConfig));
    }

    @PostMapping("admin/commission/getCommissionConfig")
    @AdminNeedLogin
    public ResultBody getCommissionConfig() {
        return ResultBody.success(commissionConfigService.list());
    }

    @PostMapping("admin/commission/getSummaryList")
    @AdminNeedLogin
    public ResultBody getSummaryList(@RequestBody JSONObject jsonObject) {

        String startTime = jsonObject.getString("startTime");

        String endTime = jsonObject.getString("endTime");

        Integer pageIndex = jsonObject.getInteger("pageIndex");

        Integer pageSize = jsonObject.getInteger("pageSize");
        Integer uid = jsonObject.getInteger("uid");


        return ResultBody.success(summaryProxyService.lambdaQuery()
                .eq(uid != null, SummaryProxy::getUid, uid).gt(startTime != null, SummaryProxy::getStartTime, startTime)
                .lt(endTime != null, SummaryProxy::getEndTime, endTime).page(new Page<>(pageIndex, pageSize)));

    }

    @PostMapping("admin/commission/execSummary")
    @AdminNeedLogin
    public ResultBody execSummary() {
        summaryProxyService.execSummary();
        return ResultBody.success();
    }

    @PostMapping("admin/commission/execSettlement")
    @AdminNeedLogin
    public ResultBody execSettlement() {
        summaryProxyService.execSettlement();
        return ResultBody.success();
    }

    @PostMapping("admin/commission/updateSummaryProxyRecord")
    @AdminNeedLogin
    public ResultBody updateSummaryProxyRecord(@RequestBody SummaryProxy summaryProxy) {

        return ResultBody.success(summaryProxyService.updateById(summaryProxy));
    }
}
