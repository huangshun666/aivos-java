package com.zs.forex.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.dto.*;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.tools.GZipTool;
import com.zs.forex.common.tools.SnowflakeIdTool;
import com.zs.forex.common.vcenum.*;
import com.zs.forex.common.web.I18nMessageUtil;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.WebException;
import com.zs.forex.handler.ClearSelfHandler;
import com.zs.forex.handler.DataHandler;
import com.zs.forex.request.SUB_USR_CLEAR;
import com.zs.forex.service.*;
import io.nats.client.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class TradeServiceImpl implements TradeService {

    private final SymbolService symbolService;

    private final OrderRecordService orderRecordService;

    private final OrderService orderService;

    private final AccountService accountService;

    private final FormulaTool formulaTool;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserAccountRecordService userAccountRecordService;


    private final RedissonClient redissonClient;

    private final Environment environment;

    public TradeServiceImpl(SymbolService symbolService, OrderRecordService orderRecordService, OrderService orderService, AccountService accountService, FormulaTool formulaTool, StringRedisTemplate stringRedisTemplate, UserAccountRecordService userAccountRecordService, RedissonClient redissonClient, Environment environment) {
        this.symbolService = symbolService;
        this.orderRecordService = orderRecordService;
        this.orderService = orderService;
        this.accountService = accountService;
        this.formulaTool = formulaTool;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userAccountRecordService = userAccountRecordService;
        this.redissonClient = redissonClient;
        this.environment = environment;
    }

    private Connection connect;

    @PostConstruct
    public void init() {
        String url = environment.getProperty("nats.url");
        Options o = new Options.Builder().maxMessagesInOutgoingQueue(200000000).connectionName("TradeService").server(url).maxReconnects(-1).build();
        try {
            connect = Nats.connect(o);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void entrust(Order order) throws WebException {

        Symbol symbol = symbolService.getById(order.getCode());
        if (symbol.getType().equals(SymbolType.Forex.ordinal())) {
            if (!FormulaTool.isTrades(new Date())) {
                throw new WebException(RespCodeEnum.not_time);
            }
        }
        User user = RequestBodyWeb.get().getUser();
        CalculateDTO calculate = this.calculate(order);
        BigDecimal bond = calculate.getBond();
        BigDecimal dealPrice = calculate.getPrice();
        BigDecimal fee = calculate.getFee();
        order.setBond(bond);
        order.setFee(fee);
        order.setCtime(new Date());
        order.setStatus(OrderStatus.Unsold.ordinal());
        order.setUid(user.getId());
        order.setDealPrice(dealPrice);
        order.setUid(user.getId());
        order.setSerial(String.valueOf(SnowflakeIdTool.next()));
        orderService.save(order);
        if (order.getType() == OrderType.Market.ordinal()) {//市价单
            order.setLimitPrice(null);
            boolean deal = this.deal(order, true);
            log.info("entrust deal:{}", deal);
        } else {                                //限价单
            //冻结资金
            Account account = accountService.current(user.getId(), FormulaTool.currency);
            if (account.getBalance().compareTo(bond.add(fee)) < 0) {
                throw new WebException(RespCodeEnum.insufficient_balance);
            }
            account.setBalance(account.getBalance().subtract(bond.add(fee)));
            account.setUnbalance(account.getUnbalance().add(bond.add(fee)));
            accountService.changeAccount(account);
            //加入挂单队列
            boolean addLimitQueue = this.addLimitQueue(order, limit_order_Q);
            log.info("entrust addLimitQueue:{}", addLimitQueue);
        }


    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deal(Order order, boolean normal) throws WebException {
        if (normal && orderService.getById(order.getId()).getStatus() != OrderStatus.Unsold.ordinal()) {
            log.error("Deal done：{}", JSONObject.toJSONString(order));
            return false;
        }
        Symbol symbol = symbolService.getById(order.getCode());
        BigDecimal num = order.getHs().multiply(symbol.getSize());
        String base = symbol.getBase();
        String quote = symbol.getQuote();
        BigDecimal bond = order.getBond();
        BigDecimal fee = order.getFee();

        BigDecimal bondAfter, bondBefore, feeAfter, feeBefore;

        Account account = accountService.current(order.getUid(), FormulaTool.currency);
        //扣钱 添加流水
        if (order.getType() == OrderType.Market.ordinal()) {             //市价单
            if (account.getBalance().compareTo(bond.add(fee)) < 0) {
                throw new WebException(RespCodeEnum.insufficient_balance);
            }
            feeBefore = account.getBalance();
            feeAfter = account.getBalance().subtract(fee);
            bondBefore = feeAfter;
            bondAfter = bondBefore.subtract(bond);
            account.setBalance(bondAfter);
            accountService.changeAccount(account);
        } else {                                 //解冻 并且进行多退少补
            BigDecimal[] calculateBond = formulaTool.calculateBond(base, quote, num, BigDecimal.valueOf(order.getLever()));
            //解冻
            BigDecimal pevTotal = bond.add(fee);
            account.setUnbalance(account.getUnbalance().subtract(pevTotal));
            account.setBalance(account.getBalance().add(pevTotal));
            accountService.changeAccount(account);
            //重新收取
            bond = calculateBond[0];
            fee = bond.multiply(symbol.getFre());
            order.setDealPrice(calculateBond[1]);
            order.setBond(bond);
            order.setFee(fee);
            account = accountService.current(order.getUid(), FormulaTool.currency);
            if (account.getBalance().compareTo(bond.add(fee)) < 0) {
                order.setStatus(OrderStatus.Canceled.ordinal());
                RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
                order.setRemark(I18nMessageUtil.getMessage(dto == null ? "en" : dto.getLang(), RespCodeEnum.insufficient_balance.getResultCode(), "order does not meet the conditions"));
                return orderService.updateById(order);
            } else {
                feeBefore = account.getBalance();
                feeAfter = account.getBalance().subtract(fee);
                bondBefore = feeAfter;
                bondAfter = bondBefore.subtract(bond);
                account.setBalance(bondAfter);
                accountService.changeAccount(account);
            }
        }
        // 交易流水记录
        UserAccountRecord tradeRecord = new UserAccountRecord();
        tradeRecord.setCtime(new Date());
        tradeRecord.setUid(order.getUid());
        tradeRecord.setAid(account.getId());
        tradeRecord.setMoney(bond.negate());
        tradeRecord.setBefore(bondBefore);
        tradeRecord.setAfter(bondAfter);
        tradeRecord.setType(UartType.Trade.ordinal());
        tradeRecord.setRefId(order.getId().toString());

        UserAccountRecord feeRecord = new UserAccountRecord();
        feeRecord.setCtime(new Date());
        feeRecord.setUid(order.getUid());
        feeRecord.setAid(account.getId());
        feeRecord.setMoney(fee.negate());
        feeRecord.setBefore(feeBefore);
        feeRecord.setAfter(feeAfter);
        feeRecord.setType(UartType.Fee.ordinal());
        feeRecord.setRefId(order.getId().toString());

        userAccountRecordService.saveBatch(new ArrayList<>(Arrays.asList(tradeRecord, feeRecord)));
        //是否设置了止盈止损
        order.setStatus(OrderStatus.DONE.ordinal());
        order.setMtime(new Date());

        if (!ObjectUtil.isAllEmpty(order.getSl(), order.getTp())) {
            this.addLimitQueue(order, limit_order_pl_Q);
            this.setSPSL(order.getTp(), order.getSl(), order.getId());
        }
        ClearDTO dto = this.cacheClearDTO(order, symbolService.getById(order.getCode()));
        order.setDealPrice(dto.getO());
        boolean update = orderService.updateById(order);
        //更新清算数据
        this.updateLiquidationData(order.getUid());
        //订阅自身清算
        RequestDTO build = RequestDTO.builder().cmd(Cmd.sub).uid(order.getUid()).build();
        this.natsRequest(RequestPath.SUB_USR_CLEAR.name(), build);
        //清算数据
        this.clear(dto, order.getDealPrice());
        return true;
    }

    @Override
    public boolean setSPSL(BigDecimal TP, BigDecimal SL, Integer orderId) {
        Order order = orderService.getById(orderId);
        order.setSl(SL);
        order.setTp(TP);
        List<Order> limitQueueOrders = this.getLimitQueueOrders(order.getCode(), limit_order_pl_Q);
        limitQueueOrders.remove(order);
        this.addLimitQueue(order, limit_order_pl_Q);
        return this.orderService.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean settlement(Integer orderId, BigDecimal price) {

        Order order = this.orderService.getById(orderId);
        if (!order.getStatus().equals(OrderStatus.DONE.ordinal())) {
            log.info("settlement order closed");
            return false;
        }
        RLock lock = redissonClient.getLock(CodeScenes.trade_lock.getPev().concat(order.getUid().toString()));
        try {
            Symbol symbol = symbolService.getById(order.getCode());
            //计算收益
            BigDecimal[] calculatePL = formulaTool.calculatePL(symbol.getBase(), symbol.getQuote(), order.getNum(), order.getDealPrice(), price);
            //收益
            BigDecimal pl = calculatePL[0];
            //平仓价
            BigDecimal dealPrice = calculatePL[1];
            //收益率
            BigDecimal plRate = dealPrice.subtract(order.getDealPrice()).divide(dealPrice,
                    2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            //如果是卖则取反
            if (order.getDirection() == OrderMode.Short.ordinal()) {
                pl = pl.negate();
                plRate = plRate.negate();
            }
            //计算最终收益 保证金+收益
            BigDecimal bond = order.getBond();
            BigDecimal finalPl = bond.add(pl);
            Account account = accountService.current(order.getUid(), FormulaTool.currency);

            BigDecimal before = account.getBalance();
            BigDecimal after = before.add(finalPl);
            if (after.compareTo(BigDecimal.ZERO) <= 0) {
                after = BigDecimal.ZERO;
            }
            account.setBalance(after);
            //添加记录
            OrderRecord or = new OrderRecord();
            or.setCtime(new Date());
            or.setCode(order.getCode());
            or.setClosePrice(dealPrice);
            or.setUid(order.getUid());
            or.setPlRate(plRate);
            or.setPl(pl);
            or.setOrderId(orderId);
            or.setOpenPrice(order.getDealPrice());
            orderRecordService.save(or);
            //更新状态
            order.setStatus(OrderStatus.Closed.ordinal());
            orderService.updateById(order);
            boolean b = accountService.changeAccount(account);
            if (b) {
                this.delCacheClearDTO(Collections.singletonList(order));
            }
            //流水记录
            UserAccountRecord tradeRecord = new UserAccountRecord();
            tradeRecord.setCtime(new Date());
            tradeRecord.setUid(order.getUid());
            tradeRecord.setAid(account.getId());
            tradeRecord.setMoney(finalPl);
            tradeRecord.setType(UartType.Settlement.ordinal());
            tradeRecord.setBefore(before);
            tradeRecord.setAfter(after);
            tradeRecord.setRefId(orderId.toString());
            userAccountRecordService.save(tradeRecord);
            this.getLimitQueueOrders(order.getCode(), limit_order_pl_Q).remove(order);
            log.info("平仓：{},订单:{} 已结束", b, order.getId());
        } finally {
            //更新清算数据
            this.updateLiquidationData(order.getUid());
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    @Override
    public void clear(String code, BigDecimal price) {

        //清算
        Map<Integer, List<ClearDTO>> map = this.getCacheClearDTOByCode(code).stream()
                .collect(Collectors.groupingBy(ClearDTO::getUid));

        map.forEach((k, v) -> {
            ClearItemDTO build = ClearItemDTO.builder().item(v).price(price).build();
            String topic = self_handler_pev.replace("${uid}", k.toString());
            connect.publish(topic, JSONObject.toJSONString(build).getBytes(StandardCharsets.UTF_8));
        });
    }

    public void clear(ClearDTO item, BigDecimal price) {
        //上一次的清算数据
        LiquidationDTO liquidationData = this.getLiquidationData(item.getUid());
        if (liquidationData == null) return;
        //上一次的收益
        BigDecimal pevPl = item.getPl();
        //总收益减去上一次的收益
        BigDecimal pevSumPl = liquidationData.getSumPl().subtract(pevPl);
        //计算收益
        BigDecimal[] calculatePL = formulaTool.calculatePL(item.getBase(), item.getQuote(), item.getNum(), item.getO(), price);
        BigDecimal pl = calculatePL[0].setScale(4, RoundingMode.HALF_UP);
        BigDecimal itemO = calculatePL[1];
        BigDecimal plRate = itemO.subtract(item.getO()).divide(itemO, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        //如果是卖则取反
        if (item.getDirection() == OrderMode.Short.ordinal()) {
            pl = pl.negate();
            plRate = plRate.negate();
        }
        item.setPl(pl);
        item.setPlRate(plRate);
        item.setClosePrice(itemO);
        BigDecimal newSumPl = pevSumPl.add(pl);
        //更新收益
        liquidationData.setSumPl(newSumPl);
        RLock lock = redissonClient.getLock(CodeScenes.trade_lock.getPev().concat(item.getUid().toString()));
        if (!lock.isLocked()) {
            //更新持仓
            this.updateCacheClearDTO(item);
            //更新爆仓数据
            this.updateLiquidationData(liquidationData);
        }
        //验证是否爆仓
        this.liquidation(liquidationData);

    }

    @Override
    public void liquidation(LiquidationDTO liquidationData) {

        Integer uid = liquidationData.getUid();
        //计算总收益
        BigDecimal sumPl = liquidationData.getSumPl();
        //计算总保证金
        BigDecimal sumBond = liquidationData.getSumBond();
        //用户当前的余额
        Account account = this.accountService.current(uid, FormulaTool.currency);
        //净值
        BigDecimal netWorth = account.getBalance().add(sumBond).add(sumPl);

        if (sumBond.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal riskRate = netWorth.divide(sumBond, 8, RoundingMode.HALF_UP).multiply(BigDecimal.TEN.pow(2));
        //是否爆仓
        boolean flag = riskRate.compareTo(BigDecimal.TEN.pow(2)) < 0;

        //处理强平
        if (flag) {
            List<ClearDTO> clearDTOList = this.getCacheClearDTOByUid(uid.toString());
            log.info("用户:{} 爆仓了~ 仓位ids:{}", uid, clearDTOList.stream().map(item -> String.valueOf(item.getOrderId())).collect(Collectors.joining(",")));
            clearDTOList.forEach(item -> this.settlement(item.getOrderId(), item.getClosePrice()));
        }


    }


    @Override
    public void handleLimitOrder(String code, BigDecimal price) {
        //处理挂单
        Map<Integer, List<Order>> collect = this.getLimitQueueOrders(code, limit_order_Q).stream().collect(Collectors.groupingBy(Order::getDirection));
        List<Order> dealOrderList = new ArrayList<>();
        collect.forEach((k, v) -> {
            List<Order> orders;
            if (k == OrderMode.Long.ordinal()) {
                orders = v.stream().filter(item -> item.getLimitPrice().compareTo(price) >= 0).collect(Collectors.toList());
            } else {
                orders = v.stream().filter(item -> item.getLimitPrice().compareTo(price) <= 0).collect(Collectors.toList());
            }
            dealOrderList.addAll(orders);
        });
        if (!dealOrderList.isEmpty()) {
            log.info("限价单成交数量 {},成交价:{}", dealOrderList.size(), price);
            dealOrderList.forEach(item -> {
                try {
                    item.setDealPrice(price);
                    this.deal(item, true);
                } catch (Exception e) {
                    log.error("挂单出现问题了！{}", JSONObject.toJSONString(e));
                }
            });
            this.getLimitQueueOrders(code, limit_order_Q).removeAll(dealOrderList);
            log.info("限价单剩余数量{}", this.getLimitQueueOrders(code, limit_order_Q).size());
        }

        //处理止盈止损单
        dealOrderList.clear();
        Map<Integer, List<Order>> limitCollect = this.getLimitQueueOrders(code, limit_order_pl_Q).stream().collect(Collectors.groupingBy(Order::getDirection));

        limitCollect.forEach((k, v) -> {
            List<Order> orders;
            //基本逻辑：价格不等于空,并且未被锁仓
            //触发逻辑：类型：多 (空相反)
            //        止盈价<=现价       例如  止盈价= 3 现价涨至=3.5 or 3 （触发）
            //        止损价>=现价            止损价= 3  现价涨至=3 or 2.9 （触发）
            if (k == OrderMode.Long.ordinal()) {
                orders = v.stream().filter(item -> (item.getTp() != null && item.getTp().compareTo(price) <= 0) && item.getLock() != LockType.Yes.ordinal() || (item.getSl() != null && item.getSl().compareTo(price) >= 0) && item.getLock() != LockType.Yes.ordinal()).collect(Collectors.toList());
            } else {
                orders = v.stream().filter(item -> (item.getTp() != null && item.getTp().compareTo(price) >= 0) && item.getLock() != LockType.Yes.ordinal() || (item.getSl() != null && item.getSl().compareTo(price) <= 0 && item.getLock() != LockType.Yes.ordinal())).collect(Collectors.toList());
            }
            dealOrderList.addAll(orders);
        });

        if (!dealOrderList.isEmpty()) {
            log.info("止盈止损单成交数量 {},成交价:{} 证券:{}", dealOrderList.size(), price, code);
            dealOrderList.forEach(item -> this.settlement(item.getId(), price));
            this.getLimitQueueOrders(code, limit_order_pl_Q).removeAll(dealOrderList);
            log.info("止盈止损单剩余数量 {},成交价:{} 证券:{}", this.getLimitQueueOrders(code, limit_order_pl_Q).size(), price, code);
        }
    }

    @Override
    public List<PositionDTO> orderList(List<Order> orderList) {

        List<PositionDTO> positionDTOS = new ArrayList<>();
        orderList.forEach(item -> {
            //证券详情
            Symbol symbol = symbolService.getById(item.getCode());
            ClearDTO clearDTO = null;

            if (item.getStatus() == OrderStatus.DONE.ordinal()) {
                clearDTO = this.getCacheClearDTOByUid(item.getUid().toString(), item.getId().toString());
            }
            //收益详情
            if (item.getStatus() == OrderStatus.Closed.ordinal()) {

                OrderRecord orderRecord = this.orderRecordService.lambdaQuery().eq(OrderRecord::getOrderId, item.getId()).last("limit 1").one();
                if (orderRecord != null && symbol != null) {
                    clearDTO = ClearDTO.builder().closePrice(orderRecord.getClosePrice()).code(item.getCode())
                            .orderId(item.getId()).num(item.getNum()).o(item.getDealPrice()).base(symbol.getBase())
                            .bond(item.getBond()).pl(orderRecord.getPl()).plRate(orderRecord.getPlRate()).uid(item.getUid())
                            .build();
                } else {
                    log.info("error {}", item.getId());
                }


            }
            //封装参数
            PositionDTO build = PositionDTO.builder().order(item).clearDTO(clearDTO).symbol(symbol).build();

            positionDTOS.add(build);
        });

        return positionDTOS;
    }

    @Override
    public CalculateDTO calculate(Order order) {
        Symbol symbol = symbolService.getById(order.getCode());
        String base = symbol.getBase();
        String quote = symbol.getQuote();
        BigDecimal num = order.getHs().multiply(symbol.getSize());
        order.setNum(num);
        //计算保证金 和 手续费
        BigDecimal[] calculateBond =
                formulaTool.calculateBond(base, quote, num, BigDecimal.valueOf(order.getLever()));
        BigDecimal bond = calculateBond[0];
        BigDecimal dealPrice = calculateBond[1];
        BigDecimal fee = bond.multiply(symbol.getFre());
        return CalculateDTO.builder().price(dealPrice).bond(bond).fee(fee.setScale(2, RoundingMode.HALF_UP)).build();
    }

    @Override
    public void canceled(Integer orderId) throws WebException {
        Order order = orderService.lambdaQuery().eq(Order::getId, orderId).eq(Order::getUid, RequestBodyWeb.get().getUser().getId()).last("limit 1").one();
        if (order == null || order.getStatus() != OrderStatus.Unsold.ordinal()) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        getLimitQueueOrders(order.getCode(), limit_order_Q).remove(order);
        order.setStatus(OrderStatus.Canceled.ordinal());
        order.setRemark(I18nMessageUtil.getMessage(RequestBodyWeb.get().getLang(), CodeScenes.normal_cancel_order.getCode(), ""));
        orderService.updateById(order);
        Account account = accountService.current(order.getUid(), FormulaTool.currency);
        account.setUnbalance(account.getUnbalance().subtract(order.getBond().add(order.getFee())));
        BigDecimal balance = account.getBalance();
        BigDecimal after = balance.add(order.getBond().add(order.getFee()));
        account.setBalance(after);
        accountService.changeAccount(account);
        //流水记录
        UserAccountRecord cancelRecord = new UserAccountRecord();
        cancelRecord.setCtime(new Date());
        cancelRecord.setUid(order.getUid());
        cancelRecord.setAid(account.getId());
        cancelRecord.setMoney(order.getBond());
        cancelRecord.setType(UartType.Cancel.ordinal());
        cancelRecord.setBefore(balance);
        cancelRecord.setAfter(after);
        cancelRecord.setRefId(orderId.toString());
        userAccountRecordService.save(cancelRecord);
    }

    @Override
    public void initLoadLimitOrder() {
        //挂单
        List<Order> limitList = orderService.lambdaQuery().eq(Order::getStatus, OrderStatus.Unsold.ordinal()).eq(Order::getType, OrderType.Limit.ordinal()).list();
        limitList.forEach(item -> this.addLimitQueue(item, limit_order_Q));
        //止盈止损单
        List<Order> TPList = orderService.lambdaQuery().isNotNull(Order::getTp).eq(Order::getStatus, OrderStatus.DONE.ordinal()).list();
        List<Order> SLList = orderService.lambdaQuery().isNotNull(Order::getSl).eq(Order::getStatus, OrderStatus.DONE.ordinal()).list();
        SLList.addAll(TPList);
        List<Integer> collect = SLList.stream().map(Order::getId).distinct().collect(Collectors.toList());
        List<Order> list;
        if (!collect.isEmpty()) {
            list = orderService.lambdaQuery().in(Order::getId, collect).list();
            list.forEach(item -> this.addLimitQueue(item, limit_order_pl_Q));

        }
    }

    @Override
    public boolean addLimitQueue(Order order, Map<String, List<Order>> map) {
        if (map.containsKey(order.getCode())) {
            map.get(order.getCode()).add(order);
        } else {
            List<Order> orderList = new ArrayList<>();
            orderList.add(order);
            map.put(order.getCode(), orderList);
        }
        return true;
    }

    @Override
    public List<Order> getLimitQueueOrders(String code, Map<String, List<Order>> map) {
        return map.get(code) == null ? new ArrayList<>() : map.get(code);
    }

    @Override
    public ClearDTO cacheClearDTO(Order order, Symbol symbol) {
        BigDecimal dif = symbol.getFre().divide(BigDecimal.TEN.pow(symbol.getPrecision()), symbol.getPrecision(), RoundingMode.DOWN);

        String codeKey = code_position_pev.replace("${code}", order.getCode());
        String uidKey = uid_position_pev.replace("${uid}", order.getUid().toString());
        BigDecimal add = order.getDirection() == OrderMode.Long.ordinal() ? order.getDealPrice().add(dif)
                : order.getDealPrice().subtract(dif);
        ClearDTO dto = ClearDTO.builder().bond(order.getBond()).num(order.getNum()).code(order.getCode()).orderId(order.getId()).o(add).uid(order.getUid()).closePrice(order.getDealPrice()).pl(BigDecimal.ZERO).plRate(BigDecimal.ZERO).base(symbol.getBase()).quote(symbol.getQuote()).direction(order.getDirection()).build();
        stringRedisTemplate.opsForHash().put(codeKey, dto.getOrderId().toString(), JSONObject.toJSONString(dto));
        stringRedisTemplate.opsForHash().put(uidKey, dto.getOrderId().toString(), JSONObject.toJSONString(dto));
        return dto;
    }

    @Override
    public List<ClearDTO> getCacheClearDTOByCode(String code) {
        String codeKey = code_position_pev.replace("${code}", code);
        String res = stringRedisTemplate.opsForHash().values(codeKey).toString();
        return JSONArray.parseArray(res).toJavaList(ClearDTO.class);
    }


    @Override
    public boolean existClearDTOByCode(String code, String orderId) {
        String codeKey = code_position_pev.replace("${code}", code);
        return stringRedisTemplate.opsForHash().hasKey(codeKey, orderId);
    }


    @Override
    public ClearDTO getCacheClearDTOByUid(String uid, String orderId) {
        String uidKey = uid_position_pev.replace("${uid}", uid);
        Object o = stringRedisTemplate.opsForHash().get(uidKey, orderId);
        return o == null ? null : JSONObject.parseObject(o.toString(), ClearDTO.class);
    }

    @Override
    public List<ClearDTO> getCacheClearDTOByUid(String uid) {
        String uidKey = uid_position_pev.replace("${uid}", uid);
        String res = stringRedisTemplate.opsForHash().values(uidKey).toString();
        return JSONArray.parseArray(res).toJavaList(ClearDTO.class);
    }

    @Override
    public void updateCacheClearDTO(ClearDTO clearDTO) {
        String codeKey = code_position_pev.replace("${code}", clearDTO.getCode());
        String uidKey = uid_position_pev.replace("${uid}", clearDTO.getUid().toString());
        stringRedisTemplate.opsForHash().put(codeKey, clearDTO.getOrderId().toString(), JSONObject.toJSONString(clearDTO));
        stringRedisTemplate.opsForHash().put(uidKey, clearDTO.getOrderId().toString(), JSONObject.toJSONString(clearDTO));
        //推送持仓
        try {
            symbolService.getConnection().publish(push_position_pev.replace("${orderId}", clearDTO.getOrderId().toString()), GZipTool.compress(JSON.toJSONString(clearDTO).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("push position fail :{}", JSON.toJSONString(clearDTO));
        }
    }

    @Override
    public void delCacheClearDTO(List<Order> orderList) {
        orderList.forEach(item -> {
            String codeKey = code_position_pev.replace("${code}", item.getCode());
            String uidKey = uid_position_pev.replace("${uid}", item.getUid().toString());
            stringRedisTemplate.opsForHash().delete(codeKey, item.getId().toString());
            stringRedisTemplate.opsForHash().delete(uidKey, item.getId().toString());
        });
    }

    @Override
    public void initLiquidationData() {
        List<Integer> list = this.orderService.lambdaQuery().eq(Order::getStatus, OrderStatus.DONE.ordinal()).select(Order::getUid).list().stream().map(Order::getUid).distinct().collect(Collectors.toList());
        if (!list.isEmpty()) {
            list.forEach(this::updateLiquidationData);
            list.forEach(this::subClearSelfHandler);
        }
    }

    @Override
    public LiquidationDTO getLiquidationData(Integer uid) {
        String key = liquidation_pev.replace("${uid}", uid.toString());
        String s = stringRedisTemplate.opsForValue().get(key);
        if (s != null) {
            return JSONObject.parseObject(s, LiquidationDTO.class);
        }
        return null;
    }

    @Override
    public void updateLiquidationData(LiquidationDTO dto) {
        String key = liquidation_pev.replace("${uid}", dto.getUid().toString());
        if (dto.getSumBond().compareTo(BigDecimal.ZERO) <= 0) {
            stringRedisTemplate.delete(key);
            //订阅自身清算
            RequestDTO build = RequestDTO.builder().cmd(Cmd.unsub).uid(dto.getUid()).build();
            this.natsRequest(RequestPath.SUB_USR_CLEAR.name(), build);
            log.info("updateLiquidationData:清算结束 :{}", dto.getUid());
        } else stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(dto));
    }

    @Override
    public void updateLiquidationData(Integer uid) {
        List<ClearDTO> clearDTOList = this.getCacheClearDTOByUid(uid.toString());
        //计算总收益
        BigDecimal sumPl = clearDTOList.stream().map(ClearDTO::getPl).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        //计算总保证金
        BigDecimal sumBond = clearDTOList.stream().map(ClearDTO::getBond).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        LiquidationDTO dto = LiquidationDTO.builder().uid(uid).sumBond(sumBond).sumPl(sumPl).build();
        this.updateLiquidationData(dto);
    }

    @Override
    public void natsRequest(String topic, RequestDTO requestDTO) {

        byte[] data = JSONObject.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);

        if (requestDTO.getCmd() == Cmd.sub) {
            Future<Message> incoming = connect.request(topic, data);
            try {
                String res = new String(incoming.get(1, TimeUnit.SECONDS).getData());
                log.info("nats natsRequest res:{}", res);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        } else {
            connect.publish(topic, data);
        }
    }

    public void subClearSelfHandler(Integer uid) {
        String topic = self_handler_pev.replace("${uid}", uid.toString());
        if (!uid_dispatcher_map.containsKey(uid)) {
            Dispatcher dispatcher = connect.createDispatcher();
            dispatcher.subscribe(topic, new ClearSelfHandler(this));
            uid_dispatcher_map.put(uid, dispatcher);
        }
    }

    public void unsubClearSelfHandler(Integer uid) {
        if (uid_dispatcher_map.containsKey(uid)) {
            Dispatcher dispatcher = uid_dispatcher_map.get(uid);
            connect.closeDispatcher(dispatcher);
            uid_dispatcher_map.remove(uid);
        }
    }


    @Override
    public void internalSubscription() {

        Dispatcher dataDispatcher = connect.createDispatcher();
        dataDispatcher.subscribe(SymbolService.convert_dto, new DataHandler(this));
    }

    @Override
    public void internalRequest() {

        Dispatcher d = connect.createDispatcher();
        d.subscribe(RequestPath.SUB_USR_CLEAR.name(), new SUB_USR_CLEAR(this));


    }
}
