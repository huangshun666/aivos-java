package com.zs.self.service;

import java.util.Date;

public interface TradeTimeService {

    /**
     * 获得交易状态
     *
     * @param market
     * @return  TradeDateEnum
     */
    int getTradeStatus(String market);

    /**
     * 是否交易中
     * @param market 市场
     * @return true false
     */
    boolean isTradeTime(String market);

    Date getEndTime(String market);
}
