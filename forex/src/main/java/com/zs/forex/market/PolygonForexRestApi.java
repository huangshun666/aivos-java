package com.zs.forex.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.dto.QuoteDTO;
import com.zs.forex.common.dto.TickerDTO;
import com.zs.forex.common.pojo.Symbol;
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
public class PolygonForexRestApi {

    private final static String url = "https://api.polygon.io";

    private final static String apikey = "WPFrh5rqQE8jxWcAMEZksFYYLktPcCnq";

    /**
     * 获得最新报价
     *
     * @param base  基准货币
     * @param quote 计价货币
     * @return 报价休息 QuoteDTO
     */
    public QuoteDTO getQuoteDTO(String base, String quote) {
        String api = url.concat("/v1/last_quote/currencies/{from}/{to}".replace("{from}", base)
                .replace("{to}", quote));
        String rest = RestTool.builder().url(api)
                .addParam("apikey", apikey)
                .get()
                .sync();
        log.warn("getQuoteDTO:{}", rest);
        JSONObject last = JSONObject.parseObject(rest).getJSONObject("last");
        log.warn("getQuoteDTO:{}", last);
        QuoteDTO quoteDTO = new QuoteDTO();
        quoteDTO.setP(last.getString("bid"));
        quoteDTO.setPair(base.concat(quote));
        return quoteDTO;
    }

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
                .replace("{forexTicker}", "C:".concat(forexTicker))
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

        String api = url.concat("/v2/snapshot/locale/global/markets/forex/tickers/C:".concat(base.concat(quote)));
        String rest = RestTool.builder().url(api)
                .addParam("apikey", apikey)
                .get()
                .sync();
        JSONObject restJson = JSONObject.parseObject(rest);
        QuoteDTO quoteDTO;
        try {
            quoteDTO = restJson.getJSONObject("ticker").getJSONObject("day").toJavaObject(QuoteDTO.class);
            quoteDTO.setPair(base.concat(quote));
            JSONObject lastQuote = restJson.getJSONObject("ticker").getJSONObject("lastQuote");
            JSONObject prevDay = restJson.getJSONObject("ticker").getJSONObject("prevDay");
            BigDecimal b = lastQuote.getBigDecimal("b");
            quoteDTO.setT(lastQuote.getLong("t"));
            quoteDTO.setPevc(prevDay.getBigDecimal("c"));
            quoteDTO.setP(b.toPlainString());
            quoteDTO.setR(b.subtract(quoteDTO.getPevc()).divide(b, 6, RoundingMode.HALF_UP).multiply(BigDecimal.TEN.pow(2)
                    .setScale(2, RoundingMode.HALF_UP)));
        } catch (Exception e) {
            log.error("restJson:{}", JSONObject.toJSONString(restJson));
            return null;
        }

        return quoteDTO;
    }

    /**
     * 获得最新交易列表
     * base – 基准货币 quote – 计价货币
     */

    public List<TickerDTO> getTickerList(String base, String quote, Integer size) {
        String api = url.concat("/v3/quotes/C:".concat(base.concat("-").concat(quote)));
        String rest = RestTool.builder().url(api)
                .addParam("apikey", apikey)
                .addParam("limit", size.toString())
                .addParam("order", "desc")
                .addParam("sort", "timestamp")
                .get()
                .sync();
        List<TickerDTO> tickerDTOList = new ArrayList<>();
        JSONArray results = JSONObject.parseObject(rest).getJSONArray("results");
        for (int i = 0; i < results.size(); i++) {
            JSONObject jsonObject = results.getJSONObject(i);
            BigDecimal ask_price = jsonObject.getBigDecimal("ask_price");
            BigDecimal bid_price = jsonObject.getBigDecimal("bid_price");
            long participant_timestamp = jsonObject.getLong("participant_timestamp") / 1000000;
            TickerDTO dto = TickerDTO.builder().p(ask_price.add(bid_price).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP).toPlainString())
                    .t(participant_timestamp).build();
            tickerDTOList.add(dto);

        }

        return tickerDTOList;
    }

    /**
     * 交易对 列表
     *
     * @return symbolList
     */
    public List<Symbol> symbolList() {
        List<Symbol> symbolList = new ArrayList<>();
        String api = url.concat("/v3/reference/tickers");
        String rest = RestTool.builder().url(api)
                .addParam("apikey", apikey)
                .addParam("market", "fx")
                .addParam("active", "true")
                .addParam("limit", "1000")
                .get()
                .sync();
        JSONArray results = JSON.parseObject(rest).getJSONArray("results");
        for (int i = 0; i < results.size(); i++) {
            JSONObject object = results.getJSONObject(i);
            String ticker = object.getString("ticker").replace("C:", "");
            String quote = object.getString("currency_symbol");
            String base = object.getString("base_currency_symbol");
            Symbol symbol = new Symbol();
            symbol.setBase(base);
            symbol.setCode(ticker);
            symbol.setQuote(quote);
            symbolList.add(symbol);
        }
        return symbolList;
    }
}
