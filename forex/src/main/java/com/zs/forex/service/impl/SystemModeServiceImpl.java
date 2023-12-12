package com.zs.forex.service.impl;

import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.vcenum.SystemMode;
import com.zs.forex.handler.CoreHandler;
import com.zs.forex.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class SystemModeServiceImpl implements SystemModeService {

    private final Environment environment;

    private final SymbolService symbolService;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserService userService;

    private final TradeService tradeService;

    private final AggregatesService aggregatesService;

    @Override
    public void init() {
        String mode = environment.getProperty(system_mode);
        new Thread(CoreHandler::start).start();
        symbolService.cachePrecision();
        if (mode == null || mode.contains(SystemMode.business.name())) {
            userService.onlyCode(UserService.onlyCode, false);
        }


        if (mode == null || mode.contains(SystemMode.liquidate.name())) {
            //加载订单
            tradeService.initLoadLimitOrder();
            //处理成交订阅
            tradeService.internalSubscription();
            //清算数据
            tradeService.initLiquidationData();

            tradeService.internalRequest();

        }

        if (mode == null || mode.contains(SystemMode.quote.name())) {
            //清理重连的key
            stringRedisTemplate.delete(CodeScenes.close.getPev());
            //创建表
            symbolService.list().forEach(aggregatesService::generateTableAll);
            //创建处理k线 线程
            aggregatesService.initTask();

            // TODO 处理自控币

            //建立连接
            symbolService.initMarketConn();
            //更新内存数据的订阅
            symbolService.internalSubscription();
            //初始化近期报价数据
            symbolService.initQuoteData();
            //k线数据开始订阅
            aggregatesService.internalSubscription();
            aggregatesService.clearOldData();
            //订阅所有状态未上线的行情
            symbolService.initSubscription();


        }


        System.out.println("=============================================================================");
        System.out.printf("%15s %15s %15s %15s", SystemMode.business.name(), SystemMode.cluster.name(),
                SystemMode.liquidate.name(), SystemMode.quote.name());
        System.out.println();
        System.out.println("=============================================================================");
        System.out.format("%15s %15s %15s %15s",
                mode != null && mode.contains(SystemMode.business.name()),
                mode != null && mode.contains(SystemMode.cluster.name()),
                mode != null && mode.contains(SystemMode.liquidate.name()),
                mode != null && mode.contains(SystemMode.quote.name()));
        System.out.println();
        System.out.println("=============================================================================");

    }

}
