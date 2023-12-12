package com.zs.forex.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.vcenum.LevelType;
import com.zs.forex.common.vcenum.OnlineType;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.WebException;
import com.zs.forex.handler.CoreHandler;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import com.zs.forex.common.web.ResultBody;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
public class SymbolController {


    private final SymbolService symbolService;

    private final AggregatesService aggregatesService;


    @PostMapping("/symbol/list")
    public ResultBody list(@RequestBody JSONObject jsonObject) {
        int pageIndex = (int) jsonObject.getOrDefault("pageIndex", 1);
        int pageSize = (int) jsonObject.getOrDefault("pageSize", 10);
        Integer type = jsonObject.getInteger("type");
        List<Symbol> symbolList = symbolService.lambdaQuery()
                .eq(Symbol::getOnline, OnlineType.Yes.ordinal()).
                eq(type != null, Symbol::getType, type)
                .page(new Page<>(pageIndex, pageSize)).getRecords();
        return ResultBody.success(symbolService.quoteData(symbolList));
    }

    @PostMapping("/symbol/code")
    public ResultBody code(@RequestBody JSONObject jsonObject) {
        String code = jsonObject.getString("code").trim();
        Symbol symbol = symbolService.lambdaQuery()
                .eq(Symbol::getOnline, OnlineType.Yes.ordinal()).eq(Symbol::getCode, code).one();
        return ResultBody.success(symbolService.quoteData(Collections.singletonList(symbol)));
    }

    @PostMapping("/symbol/search")
    public ResultBody search(@RequestBody JSONObject jsonObject) {
        String code = jsonObject.getString("code").trim();
        int pageIndex = (int) jsonObject.getOrDefault("pageIndex", 1);
        int pageSize = (int) jsonObject.getOrDefault("pageSize", 10);
        return ResultBody.success(symbolService.quoteData(symbolService.lambdaQuery()
                .eq(Symbol::getOnline, OnlineType.Yes.ordinal())
                .like(Symbol::getCode, code).page(new Page<>(pageIndex, pageSize)).getRecords()));
    }

    @PostMapping("/symbol/aggregates")
    public ResultBody aggregates(@RequestBody JSONObject jsonObject) throws WebException {
        String code = jsonObject.getString("code");
        if (code == null)
            throw new WebException(RespCodeEnum.parameter_exception);
        Date startTime = new Date(jsonObject.getLong("startTime"));

        do {
            startTime = DateUtil.offsetDay(startTime, -3);
        } while (DateUtil.isWeekend(startTime));
        Date endTime = new Date(jsonObject.getLong("endTime"));
        String timespan = (String) jsonObject.getOrDefault("timespan", "minute");
        Integer limit = (Integer) jsonObject.getOrDefault("limit", 100);
        Integer multiplier = (Integer) jsonObject.getOrDefault("multiplier", 1);
        Symbol symbol = symbolService.getById(code);
        List<AggregatesDTO> data = aggregatesService.aggregatesList(symbol.getCode(), LevelType.valueOf(timespan), multiplier,
                endTime.getTime() / 1000, startTime.getTime() / 1000, limit);

        data = data == null ? new LinkedList<>() : data;

        return ResultBody.success(data);
    }

    @PostMapping("/symbol/tickList")
    public ResultBody tickList(@RequestBody JSONObject jsonObject) throws WebException {
        String base = jsonObject.getString("base");
        String quote = jsonObject.getString("quote");
        Integer size = jsonObject.getInteger("size");
        JSONObject newJSON = new JSONObject();
        newJSON.put("code", base.concat(quote));
        newJSON.put("limit", size);
        return ResultBody.success(aggregates(newJSON));
    }

    @PostMapping("/symbol/currencyList")
    public ResultBody currencyList() {

        return ResultBody.success(symbolService.list().stream().map(Symbol::getBase).distinct()
                .collect(Collectors.toList()));
    }

    /****************************************后台**************************************/

    @PostMapping("/admin/symbol/list")
    @AdminNeedLogin(minType = UserRole.director)
    public ResultBody adminList(@RequestBody JSONObject jsonObject) {
        int pageIndex = (int) jsonObject.getOrDefault("pageIndex", 1);
        int pageSize = (int) jsonObject.getOrDefault("pageSize", 10);
        String code = jsonObject.getString("code");
        Integer online = jsonObject.getInteger("online");
        Integer type = jsonObject.getInteger("type");

        Page<Symbol> page = symbolService.lambdaQuery()
                .like(Objects.nonNull(code), Symbol::getCode, code)
                .eq(Objects.nonNull(online), Symbol::getOnline, online)
                .eq(Objects.nonNull(type), Symbol::getType, type)
                .page(new Page<>(pageIndex, pageSize));

        return ResultBody.success(page);
    }

    @PostMapping("/admin/symbol/addOrUpdate")
    @AdminNeedLogin(exclude = 3)
    public ResultBody addOrUpdate(@RequestBody Symbol symbol) {
        symbolService.updatePrecision(symbol.getCode(), symbol.getPrecision());
        if (!symbolService.lambdaQuery().eq(Symbol::getCode, symbol.getCode()).exists()) {
            CoreHandler.addTask(() -> symbolService.loadQuoteData(symbol));
        }
        return ResultBody.success(symbolService.saveOrUpdate(symbol));
    }


    @PostMapping("/admin/symbol/subscription")
    @AdminNeedLogin(exclude = 3)
    public ResultBody subscription(@RequestBody Symbol symbol) {
        Symbol finalSymbol = symbolService.getById(symbol.getCode());
        symbolService.natsRequest(finalSymbol);
        return ResultBody.success();
    }

}
