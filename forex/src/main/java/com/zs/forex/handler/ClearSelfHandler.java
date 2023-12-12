package com.zs.forex.handler;


import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.ClearItemDTO;
import com.zs.forex.service.TradeService;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 清算体系处理器
 */
@Slf4j
@AllArgsConstructor
public class ClearSelfHandler implements MessageHandler {

    private final TradeService tradeService;

    @Override
    public void onMessage(Message message) {
        try {
            ClearItemDTO itemDTO = JSONObject.parseObject(new String(message.getData(),
                    StandardCharsets.UTF_8), ClearItemDTO.class);
            if (itemDTO != null) {
                itemDTO.getItem().forEach(item -> tradeService.clear(item, itemDTO.getPrice()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
