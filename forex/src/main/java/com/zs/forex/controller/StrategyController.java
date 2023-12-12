package com.zs.forex.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@AllArgsConstructor
@RestController
public class StrategyController {

    private final AggregatesService aggregatesService;

    private final SymbolService symbolService;

    @PostMapping("/admin/strategy/add")
    @AdminNeedLogin(minType = UserRole.leader, exclude = 3)
    public ResultBody addStrategy(@RequestBody JSONObject jsonObject) {
        String code = jsonObject.getString("code");
        BigDecimal num = jsonObject.getBigDecimal("num");
        Integer decimal = jsonObject.getInteger("decimal");
        if (!ObjectUtil.isAllNotEmpty(decimal, code, num, symbolService.getById(code))) {
            return ResultBody.error(RespCodeEnum.parameter_exception);
        }
        aggregatesService.addStrategy(code, num, decimal);
        return ResultBody.success();
    }

    @PostMapping("/admin/strategy/del")
    @AdminNeedLogin(minType = UserRole.leader, exclude = 3)
    public ResultBody delStrategy(@RequestBody JSONObject jsonObject) {
        String code = jsonObject.getString("code");
        if (!ObjectUtil.isAllNotEmpty(code)) {
            return ResultBody.error(RespCodeEnum.parameter_exception);
        }
        aggregatesService.delStrategy(code);
        return ResultBody.success();
    }

    @PostMapping("/admin/strategy/get")
    @AdminNeedLogin(minType = UserRole.leader)
    public ResultBody getStrategy() {

        return ResultBody.success(aggregatesService.strategyInfo());
    }


}
