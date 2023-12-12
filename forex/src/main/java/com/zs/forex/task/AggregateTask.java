package com.zs.forex.task;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.common.tools.GZipTool;
import com.zs.forex.common.vcenum.LevelType;
import com.zs.forex.service.AggregatesService;
import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AggregateTask implements Runnable {


    private final LevelType levelType;

    private final List<Integer> intervals;

    private final AggregatesService aggregatesService;

    private final Connection connection;



    public AggregateTask(LevelType levelType, AggregatesService aggregatesService,
                         List<Integer> intervals,
                         Connection connection
    ) {
        this.levelType = levelType;
        this.intervals = intervals;
        this.aggregatesService = aggregatesService;
        this.connection = connection;
    }

    private final BlockingDeque<ParseDTO> deque = new LinkedBlockingDeque<>();

    @Override
    public void run() {

        try {
            while (true) {
                ParseDTO parseDTO = deque.take();
                if (levelType == LevelType.minute) {
                    intervals.forEach(item -> {
                        DateTime dateTime = aggregatesService.parseDate(new Date(parseDTO.getTime()), item);
                        String tableName = aggregatesService.tableName(parseDTO.getSymbol(), LevelType.minute, item);
                        AggregatesDTO aggregatesDTO = aggregatesService.exchangeData(parseDTO.getPrice(), tableName,
                                dateTime.getTime() / 1000);
                        this.push(aggregatesDTO, levelType, item, parseDTO.getSymbol());
                    });
                }

                if (levelType == LevelType.hour) {
                    intervals.forEach(item -> {

                        DateTime dateTime = DateUtil.beginOfHour(new Date(parseDTO.getTime()));
                        String tableName = aggregatesService.tableName(parseDTO.getSymbol(), LevelType.hour, item);
                        AggregatesDTO aggregatesDTO = aggregatesService.exchangeData(parseDTO.getPrice(), tableName, dateTime.getTime() / 1000);
                        this.push(aggregatesDTO, levelType, item, parseDTO.getSymbol());
                    });
                }

                if (levelType == LevelType.day) {
                    intervals.forEach(item -> {
                        DateTime dateDayTime = DateUtil.beginOfDay(new Date(parseDTO.getTime()));
                        String tableDayName = aggregatesService.tableName(parseDTO.getSymbol(), LevelType.day, item);
                        AggregatesDTO aggregatesDTO = aggregatesService.exchangeData(parseDTO.getPrice(), tableDayName,
                                dateDayTime.getTime() / 1000);
                        this.push(aggregatesDTO, levelType, item, parseDTO.getSymbol());
                    });
                }

            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void push(AggregatesDTO dto, LevelType levelType, Integer interval, String symbol) {
        String topic = AggregatesService.pushAgg.replace(AggregatesService.one, symbol)
                .replace(AggregatesService.two, levelType.name())
                .replace(AggregatesService.three, interval.toString());
        connection.publish(topic, GZipTool.compress(JSONObject.toJSONString(dto), "utf-8"));
    }

    public void addData(ParseDTO parseDTO) {
        deque.add(parseDTO);
    }
}
