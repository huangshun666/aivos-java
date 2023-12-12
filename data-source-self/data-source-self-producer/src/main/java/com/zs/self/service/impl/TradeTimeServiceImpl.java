package com.zs.self.service.impl;

import com.zs.self.service.TradeTimeService;
import com.zs.self.tenum.TradeDateEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TradeTimeServiceImpl implements TradeTimeService {

    @Value("#{${HKEX}}")
    private Map<Integer, String> HKEX;

    @Value("#{${US}}")
    private Map<Integer, String> US;

    @Value("#{${MAS}}")
    private Map<Integer, String> MAS;


    private final static Map<String, Map<Integer, String>> marketTime = new HashMap<>();

    @PostConstruct
    public void init() {

        marketTime.put("HKEX", HKEX);

        marketTime.put("US", US);

        marketTime.put("MAS", MAS);
    }


    @Override
    public int getTradeStatus(String market) {

        if (isWeekend()) {
            return TradeDateEnum.cd.getCode();
        }

        Date date = new Date();

        Map<Integer, String> timeMap = marketTime.get(market);

        for (Map.Entry<Integer, String> entry :
                timeMap.entrySet()) {
            if (this.isMiddle(entry.getValue(), date)) {
                return entry.getKey();
            }
        }

        return TradeDateEnum.cq.getCode();
    }

    @Override
    public boolean isTradeTime(String market) {
        if (isWeekend()) {
            return false;
        }

        Date date = new Date();

        Map<Integer, String> timeMap = marketTime.get(market);

        String m_trade = timeMap.get(TradeDateEnum.m_trade.getCode());

        String a_trade = timeMap.get(TradeDateEnum.a_trade.getCode());

        return this.isMiddle(m_trade, date) || this.isMiddle(a_trade, date);
    }

    private boolean isMiddle(String time, Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd ");
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String[] times = time.split(",");

        String pev = df.format(date).concat(times[0]);

        String sus = df.format(date).concat(times[1]);

        try {
            Date pevT = dft.parse(pev);

            Date susT = dft.parse(sus);

            // 处理美股交易时间
            long p = pevT.getTime();
            long s = susT.getTime();
            if (p > s) {
                Calendar instance = Calendar.getInstance();
                instance.setTime(susT);
                instance.add(Calendar.DATE, 1);
                susT = instance.getTime();
            }

            return date.getTime() >= p && date.getTime() <= susT.getTime();

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWeekend() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    }


    @Override
    public Date getEndTime(String market) {
        Date date = new Date();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd ");

        String[] times = marketTime.get(market).get(this.getTradeStatus(market)).split(",");

        String pev = df.format(date).concat(times[0]);

        String sus = df.format(date).concat(times[1]);

        Date pevT;
        Date susT;
        try {
            pevT = df.parse(pev);
            susT = df.parse(sus);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // 处理美股交易时间
        if (pevT.after(susT)) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(susT);
            instance.add(Calendar.DATE, 1);
            susT = instance.getTime();
        }
        return susT;
    }
}
