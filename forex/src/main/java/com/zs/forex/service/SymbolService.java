package com.zs.forex.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.common.dto.QuoteDTO;
import com.zs.forex.common.dto.SymbolDTO;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.vcenum.SymbolType;
import com.zs.forex.market.PolygonMarketWssApi;
import io.nats.client.Connection;

import javax.xml.crypto.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface SymbolService extends IService<Symbol> {

    String symTable = "symbol_table";

    String ws_quote = "ws.quote";

    String convert_dto = "convert.dto";

    String one_day_key = "one_day_key_${symbol}_${time}";

    Map<Integer, PolygonMarketWssApi> polygonMarketWssApiHashMap = new HashMap<>();


    String precisionKey = "precision";


    /**
     * 缓存小数精度
     */
    void cachePrecision();

    /**
     * 获得小数位
     *
     * @param code 证券
     * @return 小数位
     */
    Integer getPrecision(String code);

    /**
     * 添加或更新小数位
     */
    void updatePrecision(String code, Integer precision);

    /**
     * 首页数据
     *
     * @param symbols 证券列表
     * @return 首页数据
     */
    List<SymbolDTO> quoteData(List<Symbol> symbols);

    /**
     * 获得最新报价
     *
     * @param base  基准货币
     * @param quote 计价货币
     * @return 返回最新行情
     */
    QuoteDTO getQuote(String base, String quote);

    /**
     * 封装 real-time 数据
     *
     * @param parseDTOS
     */
    void convertQuoteDTO(List<ParseDTO> parseDTOS);

    /**
     * 加载最新行情
     *
     * @param symbol 证券
     */
    void loadQuoteData(Symbol symbol);

    /**
     * 初始化所有证券行情
     */
    void initQuoteData();

    /***
     * 清除每天的key
     */
    void clearOneDayKey();

    /**
     * 初始化连接
     */
    void initMarketConn();


    /**
     * 关闭所有连接
     */
    void closeMarketConn();

    /**
     * 检测重连
     */
    void restMarketConn();

    /**
     * 订阅 real-time 数据
     *
     * @param symbol 证券信息
     */
    void subscription(Symbol symbol);

    /**
     * 订阅行情
     * @param symbol 证券
     */
    void natsRequest(Symbol symbol);

    /**
     * 启动初始化订阅
     */
    void initSubscription();


    Connection getConnection();

    /**
     * 内部行情订阅
     */
    void internalSubscription();
}
