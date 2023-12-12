package com.zs.forex.task;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.zs.forex.common.vcenum.SystemMode;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import com.zs.forex.service.SystemModeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;


@AllArgsConstructor
@Slf4j
@Component
public class TaskList {
    private final SymbolService symbolService;

    private final AggregatesService aggregatesService;

    private final Environment environment;

    @Scheduled(cron = "0 1 0 * * ?")
    public void syncDayTicker() {
        String property = environment.getProperty(SystemModeService.system_mode);
        if (property == null || property.contains(SystemMode.quote.name())) symbolService.clearOneDayKey();

        log.warn("syncDayTicker exec successfully------");
    }


    @Scheduled(cron = "0/10 * * * * ?")
    public void verifyClose() {
        String property = environment.getProperty(SystemModeService.system_mode);
        if (property == null || property.contains(SystemMode.quote.name())) {
            //TODO 重连机制后续可以优化 哪个连接断开对哪个连接进行 重连 而不是 所有的
            symbolService.restMarketConn();
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void clearData() {
        String property = environment.getProperty(SystemModeService.system_mode);
        if (property == null || property.contains(SystemMode.quote.name())) {
            aggregatesService.clearOldData();
        }
    }

}
