package com.zs.forex.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.dto.*;
import com.zs.forex.common.mapper.SymbolMapper;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.tools.GZipTool;
import com.zs.forex.common.vcenum.BlackStatus;
import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.vcenum.RequestPath;
import com.zs.forex.common.vcenum.SymbolType;
import com.zs.forex.handler.ConvertHandler;
import com.zs.forex.handler.MarketHandler;
import com.zs.forex.handler.SelfHandler;
import com.zs.forex.market.*;
import com.zs.forex.request.SUB_SYM_QUOTE;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import io.nats.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SymbolServiceImpl extends ServiceImpl<SymbolMapper, Symbol> implements SymbolService {

    private final PolygonForexRestApi polygonForexRestApi;


    private final PolygonIndexRestApi polygonIndexRestApi;


    private final PolygonCrpytoRestApi polygonCrpytoRestApi;


    private final StringRedisTemplate stringRedisTemplate;





    @Lazy
    @Autowired
    private AggregatesService aggregatesService;
    @Value("${nats.url}")
    private String url;
    private Connection connect;

    @PostConstruct
    public void init() {
        Options o = new Options.Builder().maxMessagesInOutgoingQueue(200000000).connectionName("SymbolService").server(url).maxReconnects(-1).build();
        try {
            connect = Nats.connect(o);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cachePrecision() {
        this.list().forEach(item -> updatePrecision(item.getCode(), item.getPrecision()));
    }

    @Override
    public Integer getPrecision(String code) {
        Object o = stringRedisTemplate.opsForHash().get(precisionKey, code);
        if (o == null) return 3;
        return Integer.valueOf(o.toString());
    }

    @Override
    public void updatePrecision(String code, Integer precision) {
        stringRedisTemplate.opsForHash().put(precisionKey, code, precision.toString());
    }

    @Override
    public List<SymbolDTO> quoteData(List<Symbol> symbols) {
        return symbols.stream().map(item -> {
            SymbolDTO dto = new SymbolDTO();
            dto.setSymbol(item);
            dto.setQuoteDTO(this.getQuote(item.getBase(), item.getQuote()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public QuoteDTO getQuote(String base, String quote) {
        Object o = stringRedisTemplate.opsForHash().get(symTable, base.concat(quote));
        return o == null ? polygonForexRestApi.getQuoteDTO(base, quote) : JSONObject.parseObject(o.toString(), QuoteDTO.class);
    }

    @Override
    public void convertQuoteDTO(List<ParseDTO> parseDTOS) {
        try {
            parseDTOS.forEach(item -> {
                QuoteDTO pevQuteDTO = this.handleQuote(item.getPrice(), new Date(item.getTime()), item.getSymbol());
                stringRedisTemplate.opsForHash().put(symTable, pevQuteDTO.getPair(), JSON.toJSONString(pevQuteDTO));
                //  推送最新价
                byte[] compress = new byte[0];
                try {
                    compress = GZipTool.compress(JSON.toJSONString(pevQuteDTO).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String topic = ws_quote.concat(".").concat(pevQuteDTO.getPair());
                connect.publish(topic, compress);
            });

        } catch (Exception e) {
            e.printStackTrace();
            log.error(JSON.toJSONString(e));
        }

    }


    private QuoteDTO handleQuote(BigDecimal price, Date time, String symbol) {
        QuoteDTO pevQuteDTO;
        //更新价格
        Object obj = stringRedisTemplate.opsForHash().get(symTable, symbol);
        if (obj != null)
            pevQuteDTO = JSONObject.parseObject(obj.toString(), QuoteDTO.class);
        else
            pevQuteDTO = this.initQuoteDTO(symbol);

        DateTime dateDayTime = DateUtil.beginOfDay(time);
        String key = one_day_key.replace("${symbol}", symbol).replace("${time}", DateUtil.formatDate(dateDayTime));
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            pevQuteDTO.setO(price);
            stringRedisTemplate.opsForValue().set(key, pevQuteDTO.toString());
            pevQuteDTO.setL(price);
            pevQuteDTO.setH(price);
        }

        pevQuteDTO.setC(price);
        pevQuteDTO.setL(pevQuteDTO.getL().compareTo(price) > 0 ? price : pevQuteDTO.getL());
        pevQuteDTO.setH(pevQuteDTO.getH().compareTo(price) > 0 ? pevQuteDTO.getH() : price);

        pevQuteDTO.setP(price.toPlainString());
        pevQuteDTO.setR(price.subtract(pevQuteDTO.getO()).divide(price, 6, RoundingMode.HALF_UP));
        pevQuteDTO.setT(time.getTime() / 1000);
        pevQuteDTO.setPair(symbol);
        return pevQuteDTO;
    }

    private QuoteDTO initQuoteDTO(String pair) {
        QuoteDTO quoteDTO = new QuoteDTO();
        quoteDTO.setP(BigDecimal.ZERO.toPlainString());
        quoteDTO.setT(new Date().getTime() / 1000);
        quoteDTO.setO(BigDecimal.ZERO);
        quoteDTO.setC(BigDecimal.ZERO);
        quoteDTO.setL(BigDecimal.ZERO);
        quoteDTO.setPair(pair);
        quoteDTO.setPevc(BigDecimal.ZERO);
        quoteDTO.setR(BigDecimal.ZERO);
        quoteDTO.setH(BigDecimal.ZERO);
        return quoteDTO;
    }

    @Override
    public void loadQuoteData(Symbol symbol) {

        QuoteDTO quoteDTO = null;
        if (symbol.getType() == SymbolType.Forex.ordinal() || symbol.getType() == SymbolType.Metal.ordinal()) {
            boolean weekend = DateUtil.isWeekend(new Date());
            if (!weekend) {
                quoteDTO = polygonForexRestApi.getTicker(symbol.getBase(), symbol.getQuote());
            }
        } else if (symbol.getType() == SymbolType.Index.ordinal()) {
            //指数的
            quoteDTO = polygonIndexRestApi.getTicker(symbol.getBase(), symbol.getQuote());

        } else if (symbol.getType() == SymbolType.Crypto.ordinal()) {
            //  加密货币
            quoteDTO = polygonCrpytoRestApi.getTicker(symbol.getBase(), symbol.getQuote());
        }
        if (quoteDTO != null) {
            //处理小数位
            Integer precision = this.getPrecision(symbol.getCode());
            quoteDTO.setH(quoteDTO.getH().setScale(precision, RoundingMode.HALF_UP));
            quoteDTO.setPevc(quoteDTO.getPevc().setScale(precision, RoundingMode.HALF_UP));
            quoteDTO.setL(quoteDTO.getL().setScale(precision, RoundingMode.HALF_UP));
            quoteDTO.setP(new BigDecimal(quoteDTO.getP()).setScale(precision, RoundingMode.HALF_UP).toPlainString());
            quoteDTO.setO(quoteDTO.getO().setScale(precision, RoundingMode.HALF_UP));
            quoteDTO.setC(quoteDTO.getC().setScale(precision, RoundingMode.HALF_UP));
            stringRedisTemplate.opsForHash().put(symTable, quoteDTO.getPair(), JSON.toJSONString(quoteDTO));
        }
    }

    @Override
    public void clearOneDayKey() {
        Set<String> keys = stringRedisTemplate.keys("one_day_key_*");
        if (keys != null) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Override
    public void initQuoteData() {
        List<Symbol> list = this.lambdaQuery()
                .eq(Symbol::getHasSelf, 0).eq(Symbol::getOnline, BlackStatus.Yes.ordinal()).list();
        list.forEach(this::loadQuoteData);
    }


    @Override
    public void initMarketConn() {
        //初始化行情
        Arrays.stream(SymbolType.values()).filter(item -> item.getUrl() != null).forEach(f -> {
            PolygonMarketWssApi marketWssApi = new PolygonMarketWssApi(f.getUrl(), new MarketHandler(f, getConnection(), stringRedisTemplate));
            marketWssApi.connection();
            this.polygonMarketWssApiHashMap.put(f.ordinal(), marketWssApi);
        });
    }

    @Override
    public void closeMarketConn() {
        this.polygonMarketWssApiHashMap.values().forEach(PolygonMarketWssApi::close);

        this.polygonMarketWssApiHashMap.clear();
    }

    @Override
    public void restMarketConn() {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(CodeScenes.close.getPev()))) {
            this.closeMarketConn();

            this.initMarketConn();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.initSubscription();

            stringRedisTemplate.delete(CodeScenes.close.getPev());

            log.info("restMarketConn success！！！！");
        }
    }

    @Override
    public void subscription(Symbol symbol) {
        String cmd = null;
        PolygonMarketWssApi marketWssApi = null;
        if (symbol.getType() == SymbolType.Forex.ordinal() || symbol.getType() == SymbolType.Metal.ordinal()) {
            marketWssApi = polygonMarketWssApiHashMap.get(SymbolType.Forex.ordinal());
            String pair = symbol.getBase().concat("/").concat(symbol.getQuote());
            cmd = SymbolType.Forex.getCmd().replace(CodeScenes.pair.getPev(), pair);
        } else if (symbol.getType() == SymbolType.Index.ordinal()) {  //指数
            marketWssApi = polygonMarketWssApiHashMap.get(symbol.getType());
            cmd = SymbolType.Index.getCmd().replace(CodeScenes.pair.getPev(), symbol.getBase());
        } else if (symbol.getType() == SymbolType.Crypto.ordinal()) {
            marketWssApi = polygonMarketWssApiHashMap.get(symbol.getType());
            cmd = SymbolType.Crypto.getCmd().replace(CodeScenes.pair.getPev(),
                    symbol.getBase().concat("-").concat(symbol.getQuote()));
        }
        if (cmd != null && marketWssApi != null) marketWssApi.send(cmd);
    }


    @Override
    public void natsRequest(Symbol symbol) {
        Future<Message> incoming = connect.request(RequestPath.SUB_SYM_QUOTE.name(), JSONObject.toJSONString(symbol).getBytes(StandardCharsets.UTF_8));
        try {
            String res = new String(incoming.get(10000, TimeUnit.MILLISECONDS).getData());
            log.info("nats natsRequest res:{}", res);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initSubscription() {
        List<Symbol> list = this.lambdaQuery()
                .eq(Symbol::getOnline, BlackStatus.Yes.ordinal())

                .eq(Symbol::getHasSelf, BlackStatus.No.ordinal()).list();
        list.forEach(this::subscription);
    }

    @Override
    public Connection getConnection() {
        return connect;
    }

    public void internalSubscription() {

        Dispatcher dispatcher = connect.createDispatcher();
        dispatcher.subscribe(convert_dto, new ConvertHandler(this));

        Dispatcher dispatcherSelf = connect.createDispatcher();
        dispatcherSelf.subscribe("self_tick", new SelfHandler(stringRedisTemplate, connect));

        Dispatcher dispatcherQ = connect.createDispatcher();
        dispatcherQ.subscribe(RequestPath.SUB_SYM_QUOTE.name(), new SUB_SYM_QUOTE(this, aggregatesService));
    }
}
