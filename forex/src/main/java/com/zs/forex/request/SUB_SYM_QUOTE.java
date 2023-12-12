package com.zs.forex.request;

import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.RequestDTO;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.vcenum.Cmd;
import com.zs.forex.common.web.CommonEnum;
import com.zs.forex.handler.CoreHandler;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@AllArgsConstructor
public class SUB_SYM_QUOTE implements MessageHandler {

    private final SymbolService symbolService;

    private final AggregatesService aggregatesService;


    @Override
    public void onMessage(Message msg) {
        Runnable r = () -> {
            try {
                Symbol symbol = JSONObject.parseObject(new String(msg.getData(), StandardCharsets.UTF_8), Symbol.class);
                CoreHandler.addTask(() -> {
                    aggregatesService.generateTableAll(symbol);
                    symbolService.subscription(symbol);
                });
                msg.getConnection().publish(msg.getReplyTo(), CommonEnum.SUCCESS.name().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                msg.getConnection().publish(msg.getReplyTo(), CommonEnum.INTERNAL_SERVER_ERROR.name().getBytes(StandardCharsets.UTF_8));
                e.printStackTrace();
            }
        };
        CoreHandler.addTask(r);
    }
}
