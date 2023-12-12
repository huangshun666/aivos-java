package com.zs.forex.handler;

import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.common.dto.TickDTO;
import com.zs.forex.common.tools.GZipTool;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SelfHandler implements MessageHandler {

    private final StringRedisTemplate stringRedisTemplate;

    private final Connection connection;

    public SelfHandler(StringRedisTemplate stringRedisTemplate, Connection connection) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.connection = connection;
    }

    @Override
    public void onMessage(Message msg) throws InterruptedException {
        try {
            byte[] decompress = GZipTool.decompress(msg.getData());
            String data = new String(decompress, StandardCharsets.UTF_8);
            TickDTO tickDTO = JSONObject.parseObject(data).toJavaObject(TickDTO.class);
            List<ParseDTO> parseDTOS = this.parseDTO(tickDTO);;
            connection.publish(SymbolService.convert_dto, JSONObject.toJSONString(parseDTOS).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private List<ParseDTO> parseDTO(TickDTO tickDTO) {
        List<ParseDTO> list = new ArrayList<>();
        ParseDTO build = ParseDTO.builder().time(tickDTO.getTime())
                .symbol(tickDTO.getSymbol())
                .price(tickDTO.getPrice()).build();

        HashOperations<String, Object, Object> hash = stringRedisTemplate.opsForHash();
        if (hash.hasKey(AggregatesService.strategyPev, build.getSymbol())) {
            Object o = hash.get(AggregatesService.strategyPev, build.getSymbol());
            if (o != null) {
                BigDecimal num = new BigDecimal(o.toString().split(",")[0]);
                if (num.compareTo(BigDecimal.ZERO) != 0) {
                    int decimal = Integer.parseInt(o.toString().split(",")[1]);
                    num = num.divide(BigDecimal.TEN.pow(decimal), decimal, RoundingMode.DOWN);
                    build.setPrice(build.getPrice().add(num));
                }
            }
        }
        int precision = 3;
        Object o = stringRedisTemplate.opsForHash().get(SymbolService.precisionKey, build.getSymbol());
        if (o != null) {
            precision = Integer.parseInt(o.toString());
        }
        build.setPrice(build.getPrice().setScale(precision, RoundingMode.HALF_UP));
        list.add(build);
        return list;
    }
}
