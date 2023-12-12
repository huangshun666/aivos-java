package com.zs.forex.common.tools;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import com.zs.forex.common.dto.QuoteDTO;
import com.zs.forex.service.SymbolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;

@Component
public class FormulaTool {

    @Autowired
    private SymbolService symbolService;

    public final static String currency = "USD";

    /**
     * 计算保证金
     *
     * @param base  基准货币
     * @param quote 计价货币
     * @param num   数量
     * @param lever 杠杆
     * @return 保证金
     */
    public BigDecimal[] calculateBond(String base, String quote, BigDecimal num, BigDecimal lever) {
        BigDecimal bond = num.divide(lever, 2, RoundingMode.HALF_UP);
        BigDecimal dealPrice = new BigDecimal(symbolService.getQuote(base, quote).getP());
        if (base.equals(currency)) {
            return new BigDecimal[]{bond.setScale(2, RoundingMode.HALF_UP), dealPrice};
        } else if (quote.equals(currency)) {
            QuoteDTO dto = symbolService.getQuote(base, quote);
            dealPrice = new BigDecimal(dto.getP());
            bond = bond.multiply(dealPrice);
            return new BigDecimal[]{bond.setScale(2, RoundingMode.HALF_UP), dealPrice};
        } else {
            QuoteDTO dto;
            if (Arrays.asList("GBP,EUR,NZD,AUD".split(",")).contains(base)) {
                dto = symbolService.getQuote(base, currency);
                bond = bond.multiply(new BigDecimal(dto.getP()));
            } else {
                dto = symbolService.getQuote(currency, base);
                BigDecimal divide = BigDecimal.valueOf(1).divide(new BigDecimal(dto.getP()), 6, RoundingMode.HALF_UP);
                bond = bond.multiply(divide);
            }
            return new BigDecimal[]{bond.setScale(2, RoundingMode.HALF_UP), dealPrice};
        }
    }

    /***
     * 固定保证金方式
     * **/

    public BigDecimal[] calculateBond(String base, String quote, BigDecimal num) {
        BigDecimal fMoney = BigDecimal.valueOf(333.33);
        BigDecimal dealPrice = new BigDecimal(symbolService.getQuote(base, quote).getP());
        return new BigDecimal[]{fMoney.multiply(num), dealPrice};
    }


    /**
     * 计算收益
     *
     * @param base       基准货币
     * @param quote      计价货币
     * @param num        数量
     * @param o          开盘价
     * @param closePrice 是否自定义平仓价
     * @return 收益
     */
    public BigDecimal[] calculatePL(String base, String quote, BigDecimal num, BigDecimal o, BigDecimal closePrice) {
        QuoteDTO dto = null;
        if (closePrice == null) {
            dto = symbolService.getQuote(base, quote);
        }
        if (quote.equals(currency)) {
            BigDecimal dealPrice = closePrice == null ? new BigDecimal(dto.getP()) : closePrice;
            return new BigDecimal[]{dealPrice.subtract(o).multiply(num), dealPrice};
        } else if (base.equals(currency)) {
            BigDecimal dealPrice = closePrice == null ? new BigDecimal(dto.getP()) : closePrice;
            return new BigDecimal[]{dealPrice.subtract(o).multiply(num).divide(dealPrice, 4, RoundingMode.HALF_UP)
                    , dealPrice};
        } else {
            if (Arrays.asList("GBP,EUR,NZD,AUD".split(",")).contains(quote)) {
                QuoteDTO dtoC = symbolService.getQuote(quote, currency);
                BigDecimal dealPrice = closePrice == null ? new BigDecimal(dto.getP()) : closePrice;
                return new BigDecimal[]{dealPrice.subtract(o).multiply(num).multiply(new BigDecimal(dtoC.getP())), dealPrice};
            } else {
                QuoteDTO dtoC = symbolService.getQuote(currency, quote);
                BigDecimal divide = BigDecimal.valueOf(1).divide(new BigDecimal(dtoC.getP()), 4, RoundingMode.HALF_UP);
                BigDecimal dealPrice = closePrice == null ? new BigDecimal(dto.getP()) : closePrice;
                return new BigDecimal[]{dealPrice.subtract(o).multiply(num).multiply(divide), dealPrice};
            }
        }
    }

    public static boolean isTrades(Date date) {
        Week week = DateUtil.dayOfWeekEnum(date);
        boolean flag = Arrays.asList(Week.TUESDAY, Week.WEDNESDAY, Week.THURSDAY, Week.FRIDAY).contains(week);
        if (flag) {
            return true;
        } else {
            if (week == Week.SUNDAY)
                return false;
            if (week == Week.SATURDAY)
                return DateUtil.hour(date, true) < 6;
            if (week == Week.MONDAY)
                return DateUtil.hour(date, true) >= 5;
        }
        return false;
    }

}
