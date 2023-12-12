package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.Order;
import com.zs.forex.common.web.WebException;

import java.math.BigDecimal;

public interface OrderService extends IService<Order> {

    /**
     * 自行交易订单
     *
     * @param order 订单 信息
     * @return 是否成功
     * @throws WebException
     */
    boolean selfTrade(Order order) throws WebException;

    /**
     * 变更订单信息
     *
     * @param hs      手数
     * @param price   价格
     * @param orderId 订单ID
     * @return
     * @throws WebException
     */
    boolean changeOrder(BigDecimal hs, BigDecimal price, Integer orderId) throws WebException;
}
