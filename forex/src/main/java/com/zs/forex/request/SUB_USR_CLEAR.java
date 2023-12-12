package com.zs.forex.request;

import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.RequestDTO;
import com.zs.forex.common.vcenum.Cmd;
import com.zs.forex.common.web.CommonEnum;
import com.zs.forex.handler.CoreHandler;
import com.zs.forex.service.TradeService;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@AllArgsConstructor
public class SUB_USR_CLEAR implements MessageHandler {

    private final TradeService tradeService;

    @Override
    public void onMessage(Message msg) {
        Runnable r = () -> {
            try {
                RequestDTO requestDTO = JSONObject.parseObject(new String(msg.getData(), StandardCharsets.UTF_8), RequestDTO.class);
                if (requestDTO.getCmd() == Cmd.sub) {
                    tradeService.subClearSelfHandler(requestDTO.getUid());
                    msg.getConnection().publish(msg.getReplyTo(), CommonEnum.SUCCESS.name().getBytes(StandardCharsets.UTF_8));
                } else {
                    tradeService.unsubClearSelfHandler(requestDTO.getUid());
                }
            } catch (Exception e) {
                msg.getConnection().publish(msg.getReplyTo(), CommonEnum.INTERNAL_SERVER_ERROR.name().getBytes(StandardCharsets.UTF_8));
                e.printStackTrace();
            }
        };
        CoreHandler.addTask(r);
    }
}
