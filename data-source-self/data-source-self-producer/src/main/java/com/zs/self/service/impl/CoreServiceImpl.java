package com.zs.self.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zs.market.api.dto.Communication;
import com.zs.market.api.dto.DepthDTO;
import com.zs.market.api.dto.MarketDTO;
import com.zs.market.api.dto.TickDTO;
import com.zs.market.api.util.GZipUtils;
import com.zs.self.pojo.Model;
import com.zs.self.pojo.ModelTask;
import com.zs.self.service.CoreService;
import com.zs.self.service.ModelTaskService;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class CoreServiceImpl implements CoreService {


    @Value("${nats.url}")
    private String url;

    private Connection connect;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ModelTaskService modelTaskService;

    @PostConstruct
    public void initNatsConn() {
        Options o = new Options.Builder().discardMessagesWhenOutgoingQueueFull().maxMessagesInOutgoingQueue(20000).server(url).connectionName("com/zs/self").maxReconnects(-1).build();
        try {
            connect = Nats.connect(o);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generateData(BigDecimal price, int index, Model model, ModelTask modelTask) {
        log.warn("generateData task 111 price:{}, index:{} model:{} modelTask:{}", price, index, JSONObject.toJSONString(model), JSONObject.toJSONString(modelTask));
        if (price == null) {
            String close = stringRedisTemplate.opsForValue().get("self_market_prv_close_" + modelTask.getSymbol());
            log.warn("generateData task 222：{}", close);
            if (close != null && index > 2) {
                String[] spreadScope = modelTask.getSpreadScope().split(",");

                BigDecimal spread = RandomUtil.randomBigDecimal(new BigDecimal(spreadScope[0]),
                        new BigDecimal(spreadScope[1])).setScale(3, RoundingMode.DOWN);
                //0 加 1 不变  2 减
                int i = RandomUtil.randomInt(0, 3);
                price = new BigDecimal(close);
                if (i == 0) {
                    BigDecimal newPrice = price.add(spread);
                    if (price.compareTo(model.getHigh()) > 0) {
                        price = newPrice.subtract(spread);
                    } else {
                        price = newPrice;
                    }
                }

                if (i == 2) {
                    BigDecimal newPrice = price.subtract(spread);
                    if (price.compareTo(model.getLow()) < 0) {
                        price = newPrice.add(spread);
                    } else {
                        price = newPrice;
                    }
                }
            } else {
                log.warn("generateData task 333");
                price = RandomUtil.randomBigDecimal(model.getLow(), model.getHigh());
            }
        }

        BigDecimal newPrice = price.setScale(3, RoundingMode.DOWN);

        generateTick(index, model, modelTask, newPrice);

        generateDepth(modelTask, newPrice);
    }


    @Override
    public void generateMarket(ModelTask modelTask) {

        MarketDTO marketDTO = MarketDTO.builder().lastPrice(BigDecimal.ZERO)
                .amount(BigDecimal.ZERO).high(BigDecimal.ZERO).
                market(modelTask.getMarket())
                .chg(BigDecimal.ZERO).chgV(BigDecimal.ZERO).open(BigDecimal.ZERO).type(modelTask.getType()).low(BigDecimal.ZERO)
                .quantity(BigDecimal.ZERO).prvClose(BigDecimal.ZERO).symbol(modelTask.getSymbol()).build();


        String market_table = "self_market_${market}_${type}_${symbol}";  //maket key
        String str = JSON.toJSONString(marketDTO);
        log.info("generateMarket:{} {}", JSONObject.toJSONString(marketDTO), market_table);
        String tableName = this.convertMarkerTableName(market_table, JSONObject.parseObject(str));
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(tableName)))
            stringRedisTemplate.opsForValue().set(tableName, str, Duration.ofHours(6));
        log.info("generateMarket");
    }

    private void refurbishMarket(TickDTO tickDTO) throws Exception {

        String market_table = "self_market_${market}_${type}_${symbol}";  //maket key
        log.info("refurbishMarket: tickDTO:{}, market_table:{}", JSONObject.toJSONString(tickDTO), market_table);
        String prvClose = "self_market_prv_close_" + tickDTO.getSymbol();
        String tableName = this.convertMarkerTableName(market_table, JSONObject.parseObject(JSONObject.toJSONString(tickDTO)));

        String preMarket = stringRedisTemplate.opsForValue().get(tableName);

        if (preMarket == null) {
            log.info("refurbishMarket: 111");
            ModelTask modelTask = modelTaskService.lambdaQuery().eq(ModelTask::getMarket, tickDTO.getMarket()).eq(ModelTask::getSymbol, tickDTO.getSymbol()).one();
            this.generateMarket(modelTask);
            preMarket = stringRedisTemplate.opsForValue().get(tableName);
        }
        MarketDTO preMarketDTO = JSONObject.parseObject(preMarket, MarketDTO.class);

        preMarketDTO.setLastPrice(tickDTO.getPrice());
        if (preMarketDTO.getOpen().compareTo(BigDecimal.ZERO) == 0) {
            preMarketDTO.setOpen(tickDTO.getPrice());
        }
        if (preMarketDTO.getPrvClose().compareTo(BigDecimal.ZERO) == 0) {
            String close = stringRedisTemplate.opsForValue().get(prvClose);
            BigDecimal pevClose = tickDTO.getPrice();
            if (close != null) {
                pevClose = new BigDecimal(close);
            }
            preMarketDTO.setPrvClose(pevClose);
        }
        if (preMarketDTO.getLow().compareTo(BigDecimal.ZERO) == 0) {
            preMarketDTO.setLow(tickDTO.getPrice());
        }

        //累计交易额
        preMarketDTO.setAmount(preMarketDTO.getAmount().add(tickDTO.getAmount()));
        //累计成交量
        preMarketDTO.setQuantity(preMarketDTO.getQuantity().add(tickDTO.getQuantity()));
        //计算当日最高
        preMarketDTO.setHigh(preMarketDTO.getHigh().compareTo(tickDTO.getPrice()) < 0 ? tickDTO.getPrice() : preMarketDTO.getHigh());
        //计算当日最低
        preMarketDTO.setLow(preMarketDTO.getLow().compareTo(tickDTO.getPrice()) < 0 ? preMarketDTO.getLow() : tickDTO.getPrice());

        //股票涨跌幅=(当前最新成交价（或收盘价）-开盘参考价)÷开盘参考价×100%
        preMarketDTO.setChg(tickDTO.getPrice().subtract(preMarketDTO.getOpen()).divide(preMarketDTO.getOpen(), 4, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)));
        //涨跌值
        preMarketDTO.setChgV(tickDTO.getPrice().subtract(preMarketDTO.getOpen()));

        preMarketDTO.setTime(new Date());
        stringRedisTemplate.opsForValue().set(tableName, JSONObject.toJSONString(preMarketDTO), Duration.ofHours(6));
        log.info("refurbishMarket market 消息主题:{}", Communication.market.getTopic().concat(".").concat(tickDTO.getMarket()).concat(".").concat(tickDTO.getSymbol()).concat(".").concat(String.valueOf(tickDTO.getType())));
        // TODO 使用其他服务调用
        connect.publish(Communication.market.getTopic().concat(".").concat(tickDTO.getMarket()).concat(".").concat(tickDTO.getSymbol()).concat(".").concat(String.valueOf(tickDTO.getType())),
                GZipUtils.compress(JSON.toJSONString(preMarketDTO).getBytes(StandardCharsets.UTF_8)));

        stringRedisTemplate.opsForValue().set(prvClose, tickDTO.getPrice().toPlainString());
    }

    private String convertMarkerTableName(String odlKey, JSONObject jsonObject) {

        return odlKey.replace("${market}", jsonObject.getString("market")).replace("${type}", jsonObject.getString("type")).replace("${symbol}", jsonObject.getString("symbol"));
    }

    private void generateTick(int index, Model model, ModelTask modelTask, BigDecimal price) {
        BigDecimal high = model.getHigh();
        BigDecimal low = model.getLow();
        BigDecimal open = model.getOpen();
        String[] amountScope = modelTask.getAmountScope().split(",");
        BigDecimal amount = BigDecimal.valueOf(RandomUtil.randomInt(Integer.parseInt(amountScope[0]), Integer.parseInt(amountScope[1])));
        if (index == 0) {
            generateT(open, amount, modelTask);
        }
        if (index == 1) {
            generateT(low, amount, modelTask);
        }
        if (index == 2) {
            generateT(high, amount, modelTask);
        }

        generateT(price, amount, modelTask);
    }

    private void generateT(BigDecimal price, BigDecimal amount, ModelTask modelTask) {
        log.warn("generateT task 111");
        TickDTO dto = TickDTO.builder().type(modelTask.getType()).quantity(amount).amount(amount.multiply(price)).market(modelTask.getMarket())
                .symbol(modelTask.getSymbol()).price(price).time(new Date().getTime()).build();
        try {
            this.refurbishMarket(dto);
            log.warn("generateT task 222");
            connect.publish(Communication.tick.topic.concat(".").concat(dto.getMarket()).concat(".")
                    .concat(dto.getSymbol()).concat(".").concat(dto.getType().toString()), GZipUtils.compress(JSON.toJSONString(dto).getBytes(StandardCharsets.UTF_8)));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void generateDepth(ModelTask modelTask, BigDecimal price) {


        List<DepthDTO.item> buy = this.generateDepthT(modelTask, false, price);

        List<DepthDTO.item> sell = this.generateDepthT(modelTask, true, price);


        DepthDTO depthDTO = DepthDTO.builder().buy(buy).sell(sell)
                .market(modelTask.getMarket()).type(modelTask.getType()
                ).time(new Date()).symbol(modelTask.getSymbol()).build();


        try {
            connect.publish(Communication.depth.topic.concat(".").concat(depthDTO.getMarket()).concat(".").concat(depthDTO.getSymbol()).concat(".").concat(String.valueOf(depthDTO.getType())), GZipUtils.compress(JSON.toJSONString(depthDTO).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<DepthDTO.item> generateDepthT(ModelTask modelTask, boolean isAdd, BigDecimal price) {

        String[] amountScope = modelTask.getAmountScope().split(",");

        String[] spreadScope = modelTask.getSpreadScope().split(",");

        List<DepthDTO.item> cur = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            BigDecimal amount = BigDecimal.valueOf(RandomUtil.randomInt(Integer.parseInt(amountScope[0]), Integer.parseInt(amountScope[1])));

            BigDecimal spread = RandomUtil.randomBigDecimal(new BigDecimal(spreadScope[0]), new BigDecimal(spreadScope[1])).setScale(3, RoundingMode.DOWN);
            ;
            if (i == 0) {
                DepthDTO.item item = new DepthDTO.item();
                BigDecimal bigDecimal = isAdd ? price.add(spread) : price.subtract(spread);
                item.setAmount(bigDecimal.multiply(amount));
                item.setQuantity(amount);
                item.setPrice(bigDecimal);
                cur.add(item);
            } else {
                int j = i - 1;
                DepthDTO.item item = cur.get(j);
                DepthDTO.item itemJ = new DepthDTO.item();
                itemJ.setAmount((isAdd ? item.getPrice().add(spread) : item.getPrice().subtract(spread)).multiply(amount));
                itemJ.setQuantity(amount);
                itemJ.setPrice((isAdd ? item.getPrice().add(spread) : item.getPrice().subtract(spread)));
                cur.add(itemJ);
            }
        }
        return cur;
    }
}
