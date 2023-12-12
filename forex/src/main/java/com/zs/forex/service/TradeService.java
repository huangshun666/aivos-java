package com.zs.forex.service;

import com.zs.forex.common.dto.*;
import com.zs.forex.common.pojo.Order;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.web.WebException;
import io.nats.client.Dispatcher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface TradeService {

    Map<String, List<Order>> limit_order_Q = new ConcurrentHashMap<>();

    Map<String, List<Order>> limit_order_pl_Q = new ConcurrentHashMap<>();

    Map<Integer, Dispatcher> uid_dispatcher_map = new ConcurrentHashMap<>();

    String code_position_pev = "position_${code}";

    String uid_position_pev = "position_${uid}";

    String liquidation_pev = "liquidation_${uid}";

    String self_handler_pev = "self_handler_${uid}";

    String entrust_lock_pev = "entrust_lock_pev_${uid}";

    String push_position_pev = "ws.position.${orderId}";


    /**
     * 委托下单
     *
     * @param order 订单信息
     */
    void entrust(Order order) throws WebException;

    /**
     * 订单成交
     *
     * @param order 订单信息
     * @return 是否交易成功
     */
    boolean deal(Order order, boolean normal) throws WebException;

    /**
     * 设置止盈 止损
     *
     * @param TP      止盈
     * @param SL      止损
     * @param orderId 订单id
     * @return 是否成功
     */
    boolean setSPSL(BigDecimal TP, BigDecimal SL, Integer orderId);


    /**
     * 平仓
     *
     * @param orderId 订单id
     * @param price
     * @return 是否平仓成功
     */
    boolean settlement(Integer orderId, BigDecimal price) throws WebException;


    /**
     * 清算
     *
     * @param code  证券
     * @param price 价格
     */
    void clear(String code, BigDecimal price);

    /**
     * 清算
     *
     * @param price 价格
     */
    void clear(ClearDTO item, BigDecimal price);

    /**
     * 爆仓处理
     */
    void liquidation(LiquidationDTO dto);

    /**
     * 挂单队列
     *
     * @param order 订单信息
     */
    boolean addLimitQueue(Order order, Map<String, List<Order>> map);

    /**
     * 获得某个证券挂单队列
     *
     * @param code 证券名称
     * @return List<Order>
     */
    List<Order> getLimitQueueOrders(String code, Map<String, List<Order>> map);

    /**
     * 创建缓存 持仓
     * 缓存俩个key 证券-code 和 uid
     *
     * @param order 订单信息
     */
    ClearDTO cacheClearDTO(Order order, Symbol symbol);

    /**
     * 根据证券 获取清算对象
     *
     * @param code 证券
     */
    List<ClearDTO> getCacheClearDTOByCode(String code);

    /**
     * 根据证券 判断清算对象
     *
     * @param code 证券
     */
    boolean existClearDTOByCode(String code, String orderId);


    /**
     * 根据用户id 获取清算对象
     *
     * @param uid     证券
     * @param orderId 订单id
     */
    ClearDTO getCacheClearDTOByUid(String uid, String orderId);

    /**
     * 根据用户id 获取清算对象
     *
     * @param uid 证券
     */
    List<ClearDTO> getCacheClearDTOByUid(String uid);

    /**
     * 更新 清算对象
     *
     * @param clearDTO 对象信息
     */
    void updateCacheClearDTO(ClearDTO clearDTO);

    /**
     * 删除缓存id
     *
     * @param orderList 订单id
     */
    void delCacheClearDTO(List<Order> orderList);


    /***
     * 处理限价单
     * @param code 证券
     * @param price 最新价
     */
    void handleLimitOrder(String code, BigDecimal price);

    /**
     * 持仓数据
     *
     * @param orderList 订单列表
     * @return List<PositionDTO>
     */
    List<PositionDTO> orderList(List<Order> orderList);

    /**
     * 初始化 加载委托单 和 止盈止损单
     */
    void initLoadLimitOrder();


    /**
     * 计算预计保证金 和 手续费
     *
     * @param order 订单信息
     * @return
     */
    CalculateDTO calculate(Order order);

    /**
     * 撤单
     *
     * @param orderId 订单
     */
    void canceled(Integer orderId) throws WebException;

    /**
     * 初始化清算数据
     */
    void initLiquidationData();


    /**
     * 爆仓数据
     *
     * @param uid 用户id
     * @return 清算数据
     */
    LiquidationDTO getLiquidationData(Integer uid);


    /**
     * 更新数据
     */
    void updateLiquidationData(LiquidationDTO dto);

    /**
     * 更新数据
     */
    void updateLiquidationData(Integer uid);

    /**
     * nats 发送请求
     *
     * @param topic 请求地址
     */
    void natsRequest(String topic, RequestDTO requestDTO);

    /**
     * 订阅 用户自身清算
     *
     * @param uid 用户ID
     */
    void subClearSelfHandler(Integer uid);

    /**
     * 取消订阅 用户自身清算
     *
     * @param uid 用户ID
     */
    void unsubClearSelfHandler(Integer uid);

    /**
     * 内部清算订阅
     */
    void internalSubscription();

    /**
     * 内部请求处理
     */
    void internalRequest();
}
