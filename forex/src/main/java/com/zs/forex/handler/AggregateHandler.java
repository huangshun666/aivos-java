package com.zs.forex.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.service.AggregatesService;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;

/***
 * k线生产处理器
 */
@Slf4j
@AllArgsConstructor
public class AggregateHandler implements MessageHandler {

    private final AggregatesService aggregatesService;

    @Override
    public void onMessage(Message msg) {
        try {
            List<ParseDTO> parseDTOS = JSONArray
                    .parseArray(new String(msg.getData(), StandardCharsets.UTF_8)).toJavaList(ParseDTO.class);
            if (!parseDTOS.isEmpty())
                for (ParseDTO parseDTO : parseDTOS) {
                    log.info("AggregateHandler parseDTO:{}", JSONObject.toJSONString(parseDTO));
                    aggregatesService.loadData(parseDTO);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
