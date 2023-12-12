package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.dto.CalculateDTO;
import com.zs.forex.common.dto.ClearDTO;
import com.zs.forex.common.mapper.OrderMapper;
import com.zs.forex.common.pojo.Account;
import com.zs.forex.common.pojo.Order;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.pojo.UserAccountRecord;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.vcenum.OrderStatus;
import com.zs.forex.common.vcenum.OrderType;
import com.zs.forex.common.vcenum.UartType;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Lazy
    @Autowired
    private TradeService tradeService;

    @Lazy
    @Autowired
    private SymbolService symbolService;

    @Lazy
    @Autowired
    private AccountService accountService;
    @Lazy
    @Autowired
    private UserAccountRecordService userAccountRecordService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean selfTrade(Order order) throws WebException {
        BigDecimal bond, fee;
        CalculateDTO calculate = tradeService.calculate(order);
        bond = calculate.getBond();
        fee = calculate.getFee();
        order.setBond(bond);
        order.setFee(fee);
        order.setStatus(OrderStatus.Unsold.ordinal());
        if (order.getType().equals(OrderType.Limit.ordinal())) {
            //冻结资金
            Account account = accountService.current(order.getUid(), FormulaTool.currency);
            if (account.getBalance().compareTo(bond.add(fee)) < 0) {
                throw new WebException(RespCodeEnum.insufficient_balance);
            }
            order.setLimitPrice(order.getDealPrice());
            account.setBalance(account.getBalance().subtract(bond.add(fee)));
            account.setUnbalance(account.getUnbalance().add(bond.add(fee)));
            accountService.changeAccount(account);
            this.save(order);
            //加入挂单队列
            boolean addLimitQueue = tradeService.addLimitQueue(order, tradeService.limit_order_Q);
            log.info("entrust addLimitQueue:{}", addLimitQueue);
        } else {
            this.save(order);
            tradeService.deal(order, false);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean changeOrder(BigDecimal hs, BigDecimal price, Integer orderId) throws WebException {
        //之前的订单
        Order order = this.getById(orderId);
        hs = hs == null ? order.getHs() : hs;
        //用于计算的订单
        Order calculateOrder = new Order();
        calculateOrder.setCode(order.getCode());
        calculateOrder.setHs(hs);
        calculateOrder.setDealPrice(price);
        calculateOrder.setLever(order.getLever());
        CalculateDTO dto = tradeService.calculate(order);
        //处理是否需要 增加用户资金 或者 多扣用户金额
        BigDecimal bond = dto.getBond();
        BigDecimal pevBond = order.getBond();
        boolean flag = bond.compareTo(pevBond) >= 0;
        //处理资产
        Account current = accountService.current(order.getUid(), FormulaTool.currency);
        BigDecimal before, after, subtract;
        if (flag) {
            //需要补的钱
            subtract = bond.subtract(pevBond);
            before = current.getBalance();
            after = before.subtract(subtract);
            subtract = subtract.negate();
        } else {
            //回退用户的钱
            subtract = pevBond.subtract(bond);
            before = current.getBalance();
            after = before.add(subtract);
        }

        UserAccountRecord tradeRecord = new UserAccountRecord();
        tradeRecord.setCtime(new Date());
        tradeRecord.setUid(order.getUid());
        tradeRecord.setAid(current.getId());
        tradeRecord.setBefore(before);
        tradeRecord.setAfter(after);
        tradeRecord.setMoney(subtract);
        tradeRecord.setType(UartType.PositionChange.ordinal());
        tradeRecord.setRefId(order.getId().toString());
        userAccountRecordService.save(tradeRecord);
        current.setBalance(after);
        accountService.changeAccount(current);

        //处理仓位
        order.setBond(bond);
        order.setDealPrice(price);
        order.setHs(hs);
        Symbol symbol = symbolService.getById(order.getCode());
        order.setNum(hs.multiply(symbol.getSize()));
        Optional<ClearDTO> first = tradeService.getCacheClearDTOByUid(order.getUid().toString())
                .stream().filter(item -> item.getOrderId().equals(orderId)).findFirst();
        if (first.isPresent()) {
            boolean b = this.updateById(order);
            if (b) {
                ClearDTO clearDTO = first.get();
                clearDTO.setO(price);
                clearDTO.setNum(order.getNum());
                tradeService.updateCacheClearDTO(clearDTO);
                //更新清算数据
                tradeService.updateLiquidationData(clearDTO.getUid());
                String currPrice = symbolService.getQuote(symbol.getBase(), symbol.getQuote()).getP();
                tradeService.clear(order.getCode(), new BigDecimal(currPrice));
            }
            return b;

        }
        return false;
    }


    private CalculateDTO selfCalculate(Order order) {
        CalculateDTO calculate = CalculateDTO.builder().build();
        BigDecimal bond, fee;
        Symbol symbol = symbolService.getById(order.getCode());
        order.setNum(symbol.getSize().multiply(order.getHs()));
        if (symbol.getBase().equals(FormulaTool.currency)) {
            bond = order.getNum().divide(BigDecimal.valueOf(order.getLever()),
                    2, RoundingMode.HALF_UP);
            fee = bond.multiply(symbol.getFre());
            calculate.setBond(bond);
            calculate.setFee(fee);
        } else if (symbol.getQuote().equals(FormulaTool.currency)) {
            bond = order.getNum().divide(BigDecimal.valueOf(order.getLever()),
                    2, RoundingMode.HALF_UP);
            bond = bond.multiply(order.getDealPrice());
            fee = bond.multiply(symbol.getFre());
            calculate.setBond(bond);
            calculate.setFee(fee);
        } else {
            calculate = tradeService.calculate(order);

        }
        return calculate;
    }
}
