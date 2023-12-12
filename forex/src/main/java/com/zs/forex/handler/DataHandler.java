package com.zs.forex.handler;

import com.alibaba.fastjson.JSONArray;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.service.TradeService;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 处理止盈止损单
 * 将每个用户作为自身 handler 将仓位 和 价格推送给 ClearSelfHandler
 * 增加清算吞吐量
 * （也就是 每个 用户只清算自身数据 每个用户是一个线程）
 */
@Slf4j
@AllArgsConstructor
public class DataHandler implements MessageHandler {
    private final TradeService tradeService;

    @Override
    public void onMessage(Message message) {
        try {
            List<ParseDTO> parseDTOS = JSONArray
                    .parseArray(new String(message.getData(), StandardCharsets.UTF_8)).toJavaList(ParseDTO.class);
            if (!parseDTOS.isEmpty()) {
                parseDTOS.forEach(item -> {
                    //处理止盈 止损单
                    tradeService.handleLimitOrder(item.getSymbol(), item.getPrice());
                    //清算最新行情
                    tradeService.clear(item.getSymbol(), item.getPrice());
                    //这里处理网络粘包问题 给1ms 让数据飞一会
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
