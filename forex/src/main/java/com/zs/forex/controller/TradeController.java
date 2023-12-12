package com.zs.forex.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.aop.NeedLogin;
import com.zs.forex.common.dto.PositionDTO;
import com.zs.forex.common.pojo.Order;
import com.zs.forex.common.pojo.User;
import com.zs.forex.common.tools.SnowflakeIdTool;
import com.zs.forex.common.vcenum.*;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.OrderService;
import com.zs.forex.service.TradeService;
import com.zs.forex.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;


    @Autowired
    private RedissonClient redissonClient;

    @PostMapping("/trade/entrust")
    @NeedLogin
    public ResultBody entrust(@RequestBody Order order) throws WebException {
        if (ObjectUtil.isAllNotEmpty(order.getCode(), order.getType(), order.getDirection(), order.getHs(),
                order.getLever())) {
            if (order.getType().equals(OrderType.Limit.ordinal()) && order.getLimitPrice() == null)
                throw new WebException(RespCodeEnum.parameter_exception);
            Integer id = RequestBodyWeb.get().getUser().getId();
            if (!userService.getById(id).getAuth().equals(AuthStatus.Yes.ordinal()))
                throw new WebException(RespCodeEnum.not_certified);
            if (userService.getById(id).getBlack().equals(BlackStatus.Yes.ordinal()))
                throw new WebException(RespCodeEnum.prohibition_trading);

            RLock lock = redissonClient.getLock(tradeService.entrust_lock_pev.replace("${uid}", id.toString()));
            try {
                lock.lock();
                tradeService.entrust(order);
            } finally {
                if (lock.isLocked()) {
                    lock.unlock();
                }
            }

        } else {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        return ResultBody.success();
    }

    @PostMapping("/trade/calculate")
    public ResultBody calculate(@RequestBody Order order) throws WebException {
        if (!ObjectUtil.isAllNotEmpty(order.getCode(), order.getHs())) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        return ResultBody.success(tradeService.calculate(order));
    }

    @PostMapping("/trade/canceled")
    public ResultBody canceled(@RequestBody Order order) throws WebException {
        tradeService.canceled(order.getId());
        return ResultBody.success();
    }

    @PostMapping("/trade/setSPSL")
    @NeedLogin
    public ResultBody setSPSL(@RequestBody Order order) throws WebException {
        if (!ObjectUtil.isAllNotEmpty(order.getTp(), order.getSl()) || !orderService.lambdaQuery().eq(Order::getUid, RequestBodyWeb.get().getUser().getId()).eq(Order::getId, order.getId()).exists()) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        return ResultBody.success(tradeService.setSPSL(order.getTp(), order.getSl(), order.getId()));
    }

    @PostMapping("/trade/settlement")
    @NeedLogin
    public ResultBody settlement(@RequestBody JSONObject jsonObject) throws WebException {
        Integer orderId = jsonObject.getInteger("id");
        Order order = orderService.lambdaQuery().eq(Order::getUid, RequestBodyWeb.get().getUser().getId())
                .eq(Order::getId, orderId).last("limit 1").one();
        if (order == null)
            throw new WebException(RespCodeEnum.parameter_exception);
        if (order.getLock().equals(LockType.Yes.ordinal()))
            throw new WebException(RespCodeEnum.position_locked);
        return ResultBody.success(tradeService.settlement(orderId, null));
    }

    @PostMapping("/trade/list")
    @NeedLogin
    public ResultBody list(@RequestBody JSONObject jsonObject) throws WebException {
        int pageIndex = (int) jsonObject.getOrDefault("pageIndex", 0);
        int pageSize = (int) jsonObject.getOrDefault("pageSize", 10);
        String status = (String) jsonObject.getOrDefault("status", "1");
        Integer id = RequestBodyWeb.get().getUser().getId();
        List<Order> orderList = orderService.lambdaQuery()
                .orderByDesc(Order::getCtime).eq(Order::getUid, id).in(Order::getStatus,
                        Arrays.asList(status.split(",")))
                .page(new Page<>(pageIndex, pageSize)).getRecords();
        return ResultBody.success(tradeService.orderList(orderList));
    }

    /****************************************后台**************************************/


    @PostMapping("/admin/trade/orderList")
    @AdminNeedLogin
    public ResultBody orderList(@RequestBody JSONObject jsonObject) {
        int pageIndex = (int) jsonObject.getOrDefault("pageIndex", 0);
        int pageSize = (int) jsonObject.getOrDefault("pageSize", 10);
        Integer status = jsonObject.getInteger("status");
        Integer type = jsonObject.getInteger("type");
        String code = jsonObject.getString("code");
        Integer uid = jsonObject.getInteger("uid");
        Page<Order> data = orderService.lambdaQuery()
                .eq(uid != null, Order::getUid, uid)
                .eq(status != null, Order::getStatus, status)
                .eq(type != null, Order::getType, type)
                .eq(code != null, Order::getCode, code).orderByDesc(Order::getCtime)
                .in(RequestBodyWeb.get().getAdminUser().getType() != UserRole.administrator.ordinal()
                        , Order::getUid, userService.proxyChain(RequestBodyWeb.get()
                                .getAdminUser().getId()))
                .page(new Page<>(pageIndex, pageSize));
        List<PositionDTO> positionDTOS = tradeService.orderList(data.getRecords());
        positionDTOS.forEach(item -> item.setUser(userService.getById(item.getOrder().getUid())));
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("total", data.getTotal());
        resMap.put("records", positionDTOS);

        return ResultBody.success(resMap);
    }


    @PostMapping("/admin/trade/generateSerial")
    @AdminNeedLogin
    public ResultBody generateSerial() {
        List<Order> list = orderService.lambdaQuery().eq(Order::getSerial, "").list();

        list.forEach(item -> item.setSerial(String.valueOf(SnowflakeIdTool.next())));

        return ResultBody.success(orderService.updateBatchById(list));
    }


}

