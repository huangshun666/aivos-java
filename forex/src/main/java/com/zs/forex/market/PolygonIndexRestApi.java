package com.zs.forex.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.dto.QuoteDTO;
import com.zs.forex.common.dto.TickerDTO;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.tools.RestTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PolygonIndexRestApi {

    private final static String url = "https://api.polygon.io";

    private final static String apikey = "WPFrh5rqQE8jxWcAMEZksFYYLktPcCnq";



    public List<AggregatesDTO> aggregatesDTOList(String indexTicker, String timespan, String from
            , String to, Integer limit, Integer multiplier) {
        try {
            String api = url.concat("/v2/aggs/ticker/{indexTicker}/range/{multiplier}/{timespan}/{from}/{to}"
                    .replace("{indexTicker}", "I:".concat(indexTicker))
                    .replace("{timespan}", timespan)
                    .replace("{from}", from)
                    .replace("{to}", to)
                    .replace("{multiplier}", multiplier.toString()));

            String rest = RestTool.builder().url(api)
                    .addParam("sort", "desc")
                    .addParam("limit", limit.toString())
                    .addParam("apikey", apikey)
                    .get()
                    .sync();
            return JSON.parseObject(rest).getJSONArray("results")
                    .toJavaList(AggregatesDTO.class).stream()
                    .peek(item -> item.setT(item.getT() / 1000)).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }

    }


    public QuoteDTO getTicker(String base, String quote) {
        String api = url.concat("/v3/snapshot/indices");

        String rest = RestTool.builder().url(api)
                .addParam("apikey", apikey)
                .addParam("ticker", "I:" + base)
                .get()
                .sync();
        JSONObject restJson = JSONObject.parseObject(rest);
        QuoteDTO quoteDTO = new QuoteDTO();
        try {
            JSONObject results = restJson.getJSONArray("results").getJSONObject(0);
            JSONObject session = results.getJSONObject("session");

            //最新价
            BigDecimal price = results.getBigDecimal("value");
            if (price.scale() > 6) {
                price = price.setScale(6, RoundingMode.HALF_UP);
            }
            quoteDTO.setP(price.toPlainString());
            //时间戳
            long time = results.getLong("last_updated") / 1000;
            quoteDTO.setT(time);
            //收盘价
            BigDecimal close = session.getBigDecimal("close");
            if (close.scale() > 6) {
                close = close.setScale(6, RoundingMode.HALF_UP);
            }
            quoteDTO.setC(close);
            //最高价
            BigDecimal high = session.getBigDecimal("high");
            if (high.scale() > 6) {
                high = high.setScale(6, RoundingMode.HALF_UP);
            }
            quoteDTO.setH(high);
            //最低价
            BigDecimal low = session.getBigDecimal("low");
            if (low.scale() > 6) {
                low = low.setScale(6, RoundingMode.HALF_UP);
            }
            quoteDTO.setL(low);
            //开盘价
            BigDecimal open = session.getBigDecimal("open");
            if (open.scale() > 6) {
                open = open.setScale(6, RoundingMode.HALF_UP);
            }
            quoteDTO.setO(open);

            //币对
            String pair = results.getString("ticker").split(":")[1].concat(FormulaTool.currency);
            quoteDTO.setPair(pair);
            //昨收
            BigDecimal previous_close = session.getBigDecimal("previous_close");
            if (previous_close.scale() > 6) {
                previous_close = previous_close.setScale(6, RoundingMode.HALF_UP);
            }
            quoteDTO.setPevc(previous_close);

            //涨跌幅
            quoteDTO.setR(price.subtract(quoteDTO.getPevc()).divide(price, 6, RoundingMode.HALF_UP).multiply(BigDecimal.TEN.pow(2)
                    .setScale(2, RoundingMode.HALF_UP)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return quoteDTO;
    }
}
