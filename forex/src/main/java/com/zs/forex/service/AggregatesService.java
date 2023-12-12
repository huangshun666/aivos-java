package com.zs.forex.service;

import cn.hutool.core.date.DateTime;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.common.dto.StrategyDTO;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.vcenum.LevelType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AggregatesService {

    String tablePev = "${symbol}_${level}_${grade}";    //表的基本组成

    String strategyPev = "strategy";          //策略key

    String one = "${symbol}";                           //参数 一

    String two = "${level}";                            //参数 二

    String three = "${grade}";                          //参数 三

    String db = "aggregate";

    String pushAgg = "ws.kline.${symbol}.${level}.${grade}";

    /**
     * 生成表 同步数据
     *
     * @param symbol
     */
    void generateTableAll(Symbol symbol);


    /**
     * 初始化生成k线任务
     */
    void initTask();

    /**
     * 创建表
     *
     * @param symbol    证券名称
     * @param levelType 类型
     * @param grade     等级
     */
    void generateTable(Symbol symbol, LevelType levelType, Integer grade);


    /**
     * 同步数据
     *
     * @param symbol    证券
     * @param limit     数量
     * @param levelType 类型
     * @param grade     等级
     * @Case AUDUSD 100 levelType.day 5
     * @Desc 表示 同步 5天的维度的k线 100根
     */
    void synchronousData(Symbol symbol, Integer limit, LevelType levelType, Integer grade);

    /**
     * 更新k线
     *
     * @param parseDTO real-time data
     */
    void loadData(ParseDTO parseDTO);

    /**
     * 获得表名
     *
     * @param symbol    证券
     * @param levelType 类型
     * @param grade     等级
     * @return 表名
     */
    String tableName(String symbol, LevelType levelType, Integer grade);

    /**
     * 策略
     *
     * @param symbol 币种
     * @param mun    数量
     * @param decimal 策略变动小数位
     */
    void addStrategy(String symbol, BigDecimal mun,Integer decimal);

    /**
     * 策略详情
     *
     * @return 变动大小
     */
    List<StrategyDTO> strategyInfo();

    /**
     * 删除策略
     *
     * @param symbol 证券
     */
    void delStrategy(String symbol);

    /**
     * 更新数据
     *
     * @param price     最新价
     * @param tableName 表名
     * @param time      时间
     * @param v         交易量
     * @return 最新的数据
     */
    AggregatesDTO exchangeData(BigDecimal price, String tableName, long time);

    /**
     * 处理分钟时间
     *
     * @param date     时间
     * @param interval 间隔
     * @return DateTime
     */
    DateTime parseDate(Date date, Integer interval);

    /**
     * 查询集合
     *
     * @param code      证券
     * @param levelType 类型
     * @param grade     维度
     * @param startTime 开始
     * @param endTime   结束
     * @return List<AggregatesDTO>
     */
    List<AggregatesDTO> aggregatesList(String code, LevelType levelType,
                                       Integer grade, long startTime, long endTime, Integer limit);

    /**
     * 内部行情订阅
     */
    void internalSubscription();


    void clearOldData();
}
