package com.zs.forex.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.MarketDTO;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import com.zs.market.api.util.GZipUtils;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class SelfCoinHandler implements MessageHandler {

    private final AggregatesService aggregatesService;
    private final SymbolService symbolService;

    // 添加一个字段用于存储期望的主题
    private final String expectedTopic = "market.US.WCCUSD.1";

    @Override
    public void onMessage(Message message) throws InterruptedException {
        // 获取消息主题
        String receivedTopic = message.getSubject();

        // 检查是否匹配期望的主题
        if (expectedTopic.equals(receivedTopic)) {
            try {
                byte[] decompress = GZipUtils.decompress(message.getData());
                log.info("SelfCoinHandler message:{}", new String(decompress));
                MarketDTO marketDTO = JSONObject.parseObject(new String(decompress), MarketDTO.class);
                ParseDTO parseDTO = ParseDTO.builder().symbol(marketDTO.getSymbol()).price(marketDTO.getLastPrice()).time(marketDTO.getTime().getTime()).build();
                aggregatesService.loadData(parseDTO);
                List<ParseDTO> list = new ArrayList<>();
                list.add(parseDTO);
                symbolService.convertQuoteDTO(list);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("Received message with unexpected topic: {}", receivedTopic);
        }
    }
}
