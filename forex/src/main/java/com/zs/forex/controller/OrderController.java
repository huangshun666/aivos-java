package com.zs.forex.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.common.pojo.Order;
import com.zs.forex.common.tools.SnowflakeIdTool;
import com.zs.forex.common.vcenum.LockType;
import com.zs.forex.common.vcenum.OrderStatus;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.OrderService;
import com.zs.forex.service.SymbolService;
import com.zs.forex.service.TradeService;
import com.zs.forex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SymbolService symbolService;

    @Autowired
    private UserService userService;


    @Autowired
    @Lazy
    private TradeService tradeService;


    @PostMapping("admin/order/lockOrderList")
    @AdminNeedLogin(exclude = 3)
    public ResultBody lockOrderList(@RequestBody JSONObject jsonObject) throws WebException {
        String ids = jsonObject.getString("ids");
        Integer lock = (Integer) jsonObject.getOrDefault("lock", LockType.Yes.ordinal());

        if (!StringUtils.hasLength(ids))
            throw new WebException(RespCodeEnum.parameter_exception);

        boolean update = orderService.lambdaUpdate().set(Order::getLock, lock)
                .eq(Order::getStatus, OrderStatus.DONE.ordinal())
                .in(Order::getId, new ArrayList<>(Arrays.asList(ids.split(",")))).update();
        if (update) {
            List<Order> orders = orderService.lambdaQuery().eq(Order::getStatus, OrderStatus.DONE.ordinal())
                    .in(Order::getId, new ArrayList<>(Arrays.asList(ids.split(","))))
                    .list();
            //更新
            orders.forEach(item -> {
                tradeService.getLimitQueueOrders(item.getCode(), TradeService.limit_order_pl_Q).remove(item);
                tradeService.addLimitQueue(item, TradeService.limit_order_pl_Q);
            });

        }
        return ResultBody.success(update);

    }

    @PostMapping("admin/order/selfTrade")
    @AdminNeedLogin(exclude = 3)
    public synchronized ResultBody selfTrade(@RequestBody Order order) throws WebException {
        order.setSerial(String.valueOf(SnowflakeIdTool.next()));
        order.setCtime(new Date());
        order.setMtime(new Date());
        order.setCode(order.getCode().toUpperCase());
        if (!ObjectUtil.isAllNotEmpty(order.getDirection(), order.getCode(),
                order.getLever(), order.getType(), order.getDealPrice()
                , order.getHs(), order.getUid())) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }

        if (symbolService.getById(order.getCode()) == null) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        if (userService.getById(order.getUid()) == null) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }

        return ResultBody.success(orderService.selfTrade(order));
    }

    @PostMapping("admin/order/changeOrder")
    @AdminNeedLogin(exclude = 3)
    public ResultBody changeOrder(@RequestBody JSONObject jsonObject) throws WebException {
        BigDecimal hs = jsonObject.getBigDecimal("hs");
        BigDecimal price = jsonObject.getBigDecimal("price");
        Integer orderId = jsonObject.getInteger("orderId");
        Order byId = orderService.getById(orderId);
        if (!ObjectUtil.isAllNotEmpty(hs, price, orderId) ||
                byId == null || !byId.getStatus().equals(OrderStatus.DONE.ordinal())) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        return ResultBody.success(orderService.changeOrder(hs, price, orderId));
    }


}
