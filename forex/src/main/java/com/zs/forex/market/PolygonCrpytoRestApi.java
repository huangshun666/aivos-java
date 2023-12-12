package com.zs.forex.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.dto.QuoteDTO;
import com.zs.forex.common.tools.RestTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PolygonCrpytoRestApi {

    private final static String url = "https://api.polygon.io";

    private final static String apikey = "WPFrh5rqQE8jxWcAMEZksFYYLktPcCnq";


    /**
     * 历史 k线数据
     *
     * @param forexTicker 交易对
     * @param timespan    周期
     * @param from        开始时间
     * @param to          结算时间
     * @param limit       数量
     * @return List<AggregatesDTO>
     */

    public List<AggregatesDTO> aggregatesDTOList(String forexTicker, String timespan, String from
            , String to, Integer limit, Integer multiplier) {

        String api = url.concat("/v2/aggs/ticker/{forexTicker}/range/{multiplier}/{timespan}/{from}/{to}"
                .replace("{forexTicker}", "X:".concat(forexTicker))
                .replace("{timespan}", timespan)
                .replace("{from}", from)
                .replace("{to}", to)
                .replace("{multiplier}", multiplier.toString()));

        String rest = RestTool.builder().url(api)
                .addParam("adjusted", "true")
                .addParam("sort", "desc")
                .addParam("limit", limit.toString())
                .addParam("apikey", apikey)
                .get()
                .sync();

        return JSON.parseObject(rest).getJSONArray("results")
                .toJavaList(AggregatesDTO.class).stream()
                .peek(item -> item.setT(item.getT() / 1000)).collect(Collectors.toList());
    }

    /**
     * 获得最新行情
     * base – 基准货币 quote – 计价货币
     */

    public QuoteDTO getTicker(String base, String quote) {

        String api = url.concat("/v2/snapshot/locale/global/markets/crypto/tickers/X:"
                .concat(base.concat(quote)));
        String rest = RestTool.builder().url(api)
                .addParam("apikey", apikey)
                .get()
                .sync();
        JSONObject restJson = JSONObject.parseObject(rest);
        QuoteDTO quoteDTO;
        try {
            quoteDTO = restJson.getJSONObject("ticker").getJSONObject("day").toJavaObject(QuoteDTO.class);
            quoteDTO.setPair(base.concat(quote));
            JSONObject lastQuote = restJson.getJSONObject("ticker").getJSONObject("lastTrade");
            JSONObject prevDay = restJson.getJSONObject("ticker").getJSONObject("prevDay");
            BigDecimal b = lastQuote.getBigDecimal("p");
            quoteDTO.setT(lastQuote.getLong("t"));
            quoteDTO.setPevc(prevDay.getBigDecimal("c"));
            quoteDTO.setP(b.toPlainString());
            quoteDTO.setR(b.subtract(quoteDTO.getPevc()).divide(b, 6, RoundingMode.HALF_UP).multiply(BigDecimal.TEN.pow(2)
                    .setScale(2, RoundingMode.HALF_UP)));
        } catch (Exception e) {
           e.printStackTrace();
            return null;
        }

        return quoteDTO;
    }


}
