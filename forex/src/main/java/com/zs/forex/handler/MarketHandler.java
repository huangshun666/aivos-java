package com.zs.forex.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.ForexMsgDTO;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.vcenum.SymbolType;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * polygon 通用数据转换 通用 实体ParseDTO
 */
@Slf4j
public class MarketHandler extends WebSocketListener {


    private final static String auth = "{\"action\":\"auth\",\"params\":\"UnDpgPLtuxR2Hj8UovfQnkutHNFpW3fj\"}";

    private final StringRedisTemplate stringRedisTemplate;

    private boolean isConnectedSuccessfully = false;

    private boolean isAuthenticate = false;

    private final SymbolType symbolType;

    private final Connection connection;

    public MarketHandler(SymbolType symbolType, Connection connection, StringRedisTemplate stringRedisTemplate) {
        this.symbolType = symbolType;
        this.connection = connection;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        log.warn(" ------------- code:{},reason:{}", code, reason);
    }


    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {

        t.printStackTrace();
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(CodeScenes.close.getPev())))
            stringRedisTemplate.opsForValue().set(CodeScenes.close.getPev(), CodeScenes.close.getCode());

        log.warn(" -------------onFailure");
    }


    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        log.warn(" ------------- wss onOpen Successfully");
    }


    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {

        JSONArray jsonArray = JSONArray.parseArray(text);
        if (isConnectedSuccessfully && isAuthenticate && !isSubscribed(jsonArray)) {
            List<ParseDTO> parseDTOS = this.parseDTO(jsonArray, symbolType);
            boolean flag = true;
            //外汇不在交易时间段 不接收数据
            if (symbolType == SymbolType.Forex) {
                Date date = new Date(parseDTOS.get(0).getTime());
                flag = FormulaTool.isTrades(date);
            }
            if (flag) {
                //推送
                connection.publish(SymbolService.convert_dto, JSONObject.toJSONString(parseDTOS).getBytes(StandardCharsets.UTF_8));
            }
        }
        if (!isConnectedSuccessfully)
            this.isConnectedSuccessfully(jsonArray, webSocket);
        if (!isAuthenticate)
            this.isAuthenticate(jsonArray);


    }

    /**
     * @param connectMsg [{
     *                   "ev":"status",
     *                   "status":"connected",
     *                   "message": "Connected Successfully"
     *                   }]
     */

    private void isConnectedSuccessfully(JSONArray connectMsg, WebSocket webSocket) {
        JSONObject msg = connectMsg.getJSONObject(0);
        boolean ev = msg.containsKey("ev");
        if (ev && msg.getString("ev").equals("status")) {
            String statusValue = msg.getString("status");
            if (statusValue.equals("connected")) {
                webSocket.send(auth);
                isConnectedSuccessfully = true;
            }
        }
    }

    /**
     * @param authMsg [{
     *                "ev":"status",
     *                "status":"auth_success",
     *                "message": "authenticated"
     *                }]
     */

    private void isAuthenticate(JSONArray authMsg) {
        JSONObject msg = authMsg.getJSONObject(0);
        boolean ev = msg.containsKey("ev");
        if (ev && msg.getString("ev").equals("status")) {
            String statusValue = msg.getString("status");
            if (statusValue.equals("auth_success")) {
                isAuthenticate = true;
            }
        }
    }

    /**
     * @param SubscribedMsg [{"ev":"status","status":"success","message":"subscribed to: C.*"}]
     * @return isAuthenticate
     */
    private boolean isSubscribed(JSONArray SubscribedMsg) {
        JSONObject msg = SubscribedMsg.getJSONObject(0);
        boolean ev = msg.containsKey("ev");
        if (ev && msg.getString("ev").equals("status")) {
            String statusValue = msg.getString("status");
            return statusValue.equals("success");
        }
        return false;
    }

    //TODO 数据不应在接收端直接处理 后续要将数据放置一个 专门的处理数据的service 进行处理 因为里面包含 各种策略对价格的影响
    private  List<ParseDTO> parseDTO(JSONArray jsonArray, SymbolType symbolType) {
        List<ParseDTO> parseDTOS = new LinkedList<>();
        if (jsonArray != null) for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            ParseDTO build = ParseDTO.builder().build();
            if (symbolType == SymbolType.Forex || symbolType == SymbolType.Metal) {
                ForexMsgDTO forexMsgDTO = jsonObject.toJavaObject(ForexMsgDTO.class);
                //获取证券名称
                String symbol = forexMsgDTO.getP().replace("/", "");
                //获得均价
                BigDecimal price = forexMsgDTO.getB();
                build = ParseDTO.builder().price(price).symbol(symbol).time(forexMsgDTO.getT().getTime()).build();

            } else if (symbolType == SymbolType.Index) {  //指数
                //获取证券名称
                String p = jsonObject.getString("T");

                String symbol = p.split(":")[1].concat(FormulaTool.currency);
                //获得均价
                BigDecimal price = jsonObject.getBigDecimal("val");
                //获得价格时间
                Date time = jsonObject.getDate("t");
                if (price.scale() > 6) {
                    price = price.setScale(6, RoundingMode.HALF_UP);
                }
                build = ParseDTO.builder().price(price).symbol(symbol).time(time.getTime()).build();
            } else if (symbolType == SymbolType.Crypto) {  //加密货币
                //获取证券名称
                String p = jsonObject.getString("pair");
                String symbol = p.split("-")[0].concat(FormulaTool.currency);
                //获得均价
                BigDecimal price = jsonObject.getBigDecimal("p");
                //获得价格时间
                Date time = jsonObject.getDate("t");
                if (price.scale() > 6) {
                    price = price.setScale(6, RoundingMode.HALF_UP);
                }
                build = ParseDTO.builder().price(price).symbol(symbol).time(time.getTime()).build();
            }
            //策略处理
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
            parseDTOS.add(build);
        }
        parseDTOS = parseDTOS.stream().sorted(Comparator.comparing(ParseDTO::getTime)).collect(Collectors.toList());
        return parseDTOS;
    }
}
