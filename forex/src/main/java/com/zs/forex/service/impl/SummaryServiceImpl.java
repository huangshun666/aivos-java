package com.zs.forex.service.impl;

import cn.hutool.core.date.DateUtil;
import com.zs.forex.common.dto.SummaryDTO;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.vcenum.UartType;
import com.zs.forex.common.vcenum.UatStatus;
import com.zs.forex.common.vcenum.UatType;
import com.zs.forex.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class SummaryServiceImpl implements SummaryService {
    @Autowired
    private UserService userService;

    @Autowired
    private UserAccountRecordService userAccountRecordService;

    @Autowired
    private UserAccountTradeService userAccountTradeService;

    @Autowired
    private OrderRecordService orderRecordService;
    @Autowired
    private RewardRecordService rewardRecordService;

    @Override
    public SummaryDTO indexData(Integer uid) {
        List<Integer> list = userService.proxyChain(uid);
        String yesterday = DateUtil.yesterday().toDateStr();
        String today = DateUtil.today();

        //实际充值总金额
        BigDecimal actualTRA = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getType, UatType.Deposit.ordinal())
                .in(UserAccountTrade::getUid, list)
                .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                .select(UserAccountTrade::getMoney).list()
                .stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //充值赠送总金额
        BigDecimal giftTRA = userAccountRecordService.lambdaQuery()
                .eq(UserAccountRecord::getType, UartType.Recharge.ordinal())
                .in(UserAccountRecord::getUid, list)
                .select(UserAccountRecord::getMoney).list()
                .stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //赠送体验金总金额
        BigDecimal experienceTRA = userAccountRecordService.lambdaQuery()
                .eq(UserAccountRecord::getType, UartType.Gift.ordinal())
                .in(UserAccountRecord::getUid, list)
                .select(UserAccountRecord::getMoney).list()
                .stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        //昨日实际充值总金额
        BigDecimal yesterdayActualTRA = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getType, UatType.Deposit.ordinal())
                .in(UserAccountTrade::getUid, list)
                .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                .between(UserAccountTrade::getCtime, yesterday, today)
                .select(UserAccountTrade::getMoney).list()
                .stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //昨日充值赠送总金额
        BigDecimal yesterdayGiftTRA = userAccountRecordService.lambdaQuery()
                .eq(UserAccountRecord::getType, UartType.Recharge.ordinal())
                .in(UserAccountRecord::getUid, list)
                .between(UserAccountRecord::getCtime, yesterday, today)
                .select(UserAccountRecord::getMoney).list()
                .stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //昨日赠送体验金总金额
        BigDecimal yesterdayExperienceTRA = userAccountRecordService.lambdaQuery()
                .eq(UserAccountRecord::getType, UartType.Gift.ordinal())
                .in(UserAccountRecord::getUid, list)
                .between(UserAccountRecord::getCtime, yesterday, today)
                .select(UserAccountRecord::getMoney).list()
                .stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);


        //今日实际充值总金额
        BigDecimal dayActualTRA = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getType, UatType.Deposit.ordinal())
                .in(UserAccountTrade::getUid, list)
                .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                .ge(UserAccountTrade::getCtime, today)
                .select(UserAccountTrade::getMoney).list()
                .stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //今日充值赠送总金额
        BigDecimal dayGiftTRA = userAccountRecordService.lambdaQuery()
                .eq(UserAccountRecord::getType, UartType.Recharge.ordinal())
                .in(UserAccountRecord::getUid, list)
                .ge(UserAccountRecord::getCtime, today)
                .select(UserAccountRecord::getMoney).list()
                .stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        //今日赠送体验金总金额
        BigDecimal dayExperienceTRA = userAccountRecordService.lambdaQuery()
                .eq(UserAccountRecord::getType, UartType.Gift.ordinal())
                .in(UserAccountRecord::getUid, list)
                .ge(UserAccountRecord::getCtime, today)
                .select(UserAccountRecord::getMoney).list()
                .stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        //用户提现总金额
        BigDecimal withdrawTRA = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getType, UatType.Withdraw.ordinal())
                .in(UserAccountTrade::getUid, list)
                .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                .select(UserAccountTrade::getMoney).list()
                .stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        //今日用户提现总金额
        BigDecimal withdrawDayTRA = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getType, UatType.Withdraw.ordinal())
                .in(UserAccountTrade::getUid, list)
                .ge(UserAccountTrade::getCtime, today)
                .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                .select(UserAccountTrade::getMoney).list()
                .stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //昨日用户提现总金额；
        BigDecimal withdrawYesterdayTRA = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getType, UatType.Withdraw.ordinal())
                .in(UserAccountTrade::getUid, list)
                .between(UserAccountTrade::getCtime, yesterday, today)
                .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                .select(UserAccountTrade::getMoney).list()
                .stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //总用户数
        Integer userTotal = userService.lambdaQuery().in(User::getId, list).count().intValue();
        //今日新增用户数
        Integer userDayTotal = userService.lambdaQuery().in(User::getId, list).ge(User::getCtime, today)
                .count().intValue();
        //昨日新增用户数；
        Integer userYesterdayTotal = userService.lambdaQuery().in(User::getId, list).between(User::getCtime, yesterday, today)
                .count().intValue();
        //总有效用户数
        Integer efficientUserTotal = rewardRecordService.lambdaQuery().in(RewardRecord::getProxyId, list).count().intValue();
        //今日新增有效用户数
        Integer efficientUserDayTotal = rewardRecordService.lambdaQuery().ge(RewardRecord::getCtime, today).in(RewardRecord::getProxyId, list).count().intValue();

        //昨日新增有效用户数
        Integer efficientUserYesterdayTotal = rewardRecordService.lambdaQuery().between(RewardRecord::getCtime, yesterday, today).in(RewardRecord::getProxyId, list).count().intValue();

        //总流水
        BigDecimal turnoverTotal = userAccountRecordService.lambdaQuery().select(UserAccountRecord::getMoney)
                .list().stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //今日总流水
        BigDecimal turnoverDayTotal = userAccountRecordService.lambdaQuery().select(UserAccountRecord::getMoney)
                .ge(UserAccountRecord::getCtime, today).list().stream().map(UserAccountRecord::getMoney)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        //昨日总流水
        BigDecimal turnoverYesterdayTotal = userAccountRecordService.lambdaQuery().select(UserAccountRecord::getMoney)
                .between(UserAccountRecord::getCtime, yesterday, today).list()
                .stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        //用户交易总收益
        BigDecimal tradeTotal = orderRecordService.lambdaQuery().select(OrderRecord::getPl).list().stream().map(OrderRecord::getPl).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        //今日用户交易总收益
        BigDecimal dayTradeTotal = orderRecordService.lambdaQuery()
                .ge(OrderRecord::getCtime, today).select(OrderRecord::getPl).list().stream().map(OrderRecord::getPl).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        //昨日用户交易总收益
        BigDecimal yesterdayTradeTotal = orderRecordService.lambdaQuery()
                .between(OrderRecord::getCtime, yesterday, today).select(OrderRecord::getPl).list().stream()
                .map(OrderRecord::getPl).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        SummaryDTO summaryDTO = new SummaryDTO();
        summaryDTO.setActualTRA(actualTRA);
        summaryDTO.setGiftTRA(giftTRA);
        summaryDTO.setExperienceTRA(experienceTRA);
        summaryDTO.setYesterdayActualTRA(yesterdayActualTRA);
        summaryDTO.setYesterdayGiftTRA(yesterdayGiftTRA);
        summaryDTO.setYesterdayExperienceTRA(yesterdayExperienceTRA);
        summaryDTO.setDayActualTRA(dayActualTRA);
        summaryDTO.setDayGiftTRA(dayGiftTRA);
        summaryDTO.setDayExperienceTRA(dayExperienceTRA);
        summaryDTO.setWithdrawTRA(withdrawTRA);
        summaryDTO.setWithdrawDayTRA(withdrawDayTRA);
        summaryDTO.setWithdrawYesterdayTRA(withdrawYesterdayTRA);
        summaryDTO.setUserTotal(userTotal);
        summaryDTO.setUserDayTotal(userDayTotal);
        summaryDTO.setUserYesterdayTotal(userYesterdayTotal);
        summaryDTO.setEfficientUserTotal(efficientUserTotal);
        summaryDTO.setEfficientUserDayTotal(efficientUserDayTotal);
        summaryDTO.setEfficientUserYesterdayTotal(efficientUserYesterdayTotal);
        summaryDTO.setTurnoverTotal(turnoverTotal);
        summaryDTO.setTurnoverDayTotal(turnoverDayTotal);
        summaryDTO.setTurnoverYesterdayTotal(turnoverYesterdayTotal);
        summaryDTO.setTradeTotal(tradeTotal);
        summaryDTO.setDayTradeTotal(dayTradeTotal);
        summaryDTO.setYesterdayTradeTotal(yesterdayTradeTotal);
        return summaryDTO;
    }
}
