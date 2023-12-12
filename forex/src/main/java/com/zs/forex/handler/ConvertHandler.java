package com.zs.forex.handler;

import com.alibaba.fastjson.JSONArray;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.service.SymbolService;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 更新系统 每个证券的处理器
 */
@Slf4j
@AllArgsConstructor
public class ConvertHandler implements MessageHandler {


    private final SymbolService symbolService;


    public void onMessage(Message msg) {
        try {
            List<ParseDTO> parseDTOS = JSONArray
                    .parseArray(new String(msg.getData(), StandardCharsets.UTF_8)).toJavaList(ParseDTO.class);
            if (!parseDTOS.isEmpty())
                symbolService.convertQuoteDTO(parseDTOS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
