package com.zs.forex.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.InvitationRelationMapper;
import com.zs.forex.common.mapper.SummaryProxyMapper;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.vcenum.UartType;
import com.zs.forex.common.vcenum.UatType;
import com.zs.forex.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SummaryProxyServiceImpl extends ServiceImpl<SummaryProxyMapper, SummaryProxy> implements SummaryProxyService {

    @Autowired
    private RewardRecordService rewardRecordService;
    @Autowired
    private UserService userService;
    @Autowired
    private LevelService levelService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserAccountRecordService userAccountRecordService;

    @Autowired
    private InvitationRelationMapper invitationRelationMapper;

    @Autowired
    private CommissionConfigService commissionConfigService;

    @Autowired
    private OrderRecordService orderRecordService;

    @Override
    public void execSummary() {
        List<Integer> collect = invitationRelationMapper.selectList(new LambdaQueryWrapper<InvitationRelation>().select(InvitationRelation::getInvitationId).groupBy(InvitationRelation::getInvitationId)).stream().map(InvitationRelation::getInvitationId)
                .collect(Collectors.toList());
        if (collect.isEmpty()) return;
        CommissionConfig one = commissionConfigService.lambdaQuery().one();

        DateTime beginOfWeekTime = DateUtil.beginOfWeek(DateUtil.date());

        DateTime endOfWeekTime = DateUtil.endOfWeek(DateUtil.date());

        List<SummaryProxy> summaryProxyList = collect.stream().map(item -> {
            User user = userService.getById(item);
            String proxyCode = user.getRelationCode();   // 用户邀请码
            Integer pevProxyId = null;  //  推荐人id

            InvitationRelation invitationRelation = invitationRelationMapper.selectOne(new LambdaQueryWrapper<InvitationRelation>()
                    .eq(InvitationRelation::getUid, item).last("limit 1"));
            if (invitationRelation != null) {
                pevProxyId = invitationRelation.getInvitationId();
            }
            Level levelNext = levelService.getLevel(item, false);
            int level;       //代理等级
            if (levelNext == null) {
                level = 0;
            } else {
                level = Integer.parseInt(levelNext.getMark());
            }
            List<Integer> list = new ArrayList<>();
            this.recursionAllUid(item, list);
            Long count = rewardRecordService.lambdaQuery().in(RewardRecord::getUid, list).count();
            Integer group = count.intValue();       //整个团队有效人数

            String beginOfWeek = beginOfWeekTime.toDateStr();

            String endOfWeek = endOfWeekTime.toDateStr();

            //本周总充值
            BigDecimal deposit = userAccountRecordService.lambdaQuery().in(UserAccountRecord::getType, UartType.Deposit.ordinal(), UartType.Recharge.ordinal())
                    .in(UserAccountRecord::getUid, list).between(UserAccountRecord::getCtime, beginOfWeek, endOfWeek).select(UserAccountRecord::getMoney).list().stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            //本周总提现
            BigDecimal withdraw = userAccountRecordService.lambdaQuery().eq(UserAccountRecord::getType, UartType.Withdraw.ordinal()).in(UserAccountRecord::getUid, list)
                    .between(UserAccountRecord::getCtime, beginOfWeek, endOfWeek).select(UserAccountRecord::getMoney).list().stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            // 账户余额
            BigDecimal amount = list.stream().map(cur -> accountService.current(cur, FormulaTool.currency)).map(Account::getBalance).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            //下级直推总收益
            BigDecimal nextPL = rewardRecordService.lambdaQuery().in(RewardRecord::getUid, list)
                    .select(RewardRecord::getMoney).list().stream().map(RewardRecord::getMoney)
                    .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            //下级直推总收益本周
            BigDecimal nextPLWeek = rewardRecordService.lambdaQuery().in(RewardRecord::getUid, list)
                    .select(RewardRecord::getMoney).between(RewardRecord::getCtime, beginOfWeek, endOfWeek)
                    .list().stream().map(RewardRecord::getMoney).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            List<Integer> threeUid = this.threeUid(item);
            //查询下三级 本周实际发放
            BigDecimal bigDecimal = orderRecordService.lambdaQuery().in(OrderRecord::getUid, threeUid).ge(OrderRecord::getPl, 0).select(OrderRecord::getPl).list().stream().map(OrderRecord::getPl).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            BigDecimal commissionRate = one.getCommissionRate();
            if (commissionRate == null) commissionRate = BigDecimal.valueOf(0.01);
            BigDecimal actualDistribution = bigDecimal.multiply(commissionRate);
            SummaryProxy summaryProxy = new SummaryProxy();
            summaryProxy.setEndTime(endOfWeekTime);
            summaryProxy.setStartTime(beginOfWeekTime);
            summaryProxy.setProxyCode(proxyCode);
            summaryProxy.setPevProxyId(pevProxyId);
            summaryProxy.setAmount(amount);
            summaryProxy.setDeposit(deposit);
            summaryProxy.setWithdraw(withdraw);
            summaryProxy.setGroup(group);
            summaryProxy.setLevel(level);
            summaryProxy.setNextPlWeek(nextPLWeek);
            summaryProxy.setActualDistribution(actualDistribution);
            summaryProxy.setUid(item);
            summaryProxy.setNextPl(nextPL);
            summaryProxy.setSettlement(0);
            return summaryProxy;
        }).collect(Collectors.toList());

        SummaryProxy one1 = this.lambdaQuery().orderByDesc(SummaryProxy::getId).last("limit 1").one();
        if (one1 == null || DateUtil.compare(new Date(), one1.getEndTime()) > 0)
            this.saveBatch(summaryProxyList);
        else {
            this.lambdaUpdate().eq(SummaryProxy::getSettlement, 0).remove();
            this.saveBatch(summaryProxyList);
        }

    }


    @Transactional
    @Override
    public void execSettlement() {
        List<SummaryProxy> list = this.lambdaQuery().eq(SummaryProxy::getSettlement, 0).list();
        list.stream().filter(sp -> sp.getActualDistribution().compareTo(BigDecimal.ZERO) > 0).forEach(item -> {
            BigDecimal actualDistribution = item.getActualDistribution();
            Account current = accountService.current(item.getUid(), FormulaTool.currency);

            UserAccountRecord commAwardRecord = new UserAccountRecord();
            commAwardRecord.setCtime(new Date());
            commAwardRecord.setUid(item.getUid());
            commAwardRecord.setAid(current.getId());
            commAwardRecord.setMoney(actualDistribution);
            commAwardRecord.setBefore(current.getBalance());
            commAwardRecord.setAfter(current.getBalance().add(actualDistribution));
            commAwardRecord.setType(UartType.Commission_Settlement.ordinal());
            commAwardRecord.setRefId(item.getId().toString());

            current.setBalance(current.getBalance().add(actualDistribution));
            userAccountRecordService.save(commAwardRecord);
            accountService.changeAccount(current);
            item.setSettlement(1);
        });

        this.updateBatchById(list);
    }

    private void recursionAllUid(Integer uid, List<Integer> initList) {
        List<Integer> c = userService.directProxyChain(uid);
        if (!c.isEmpty()) {
            initList.addAll(c);
            c.forEach(item -> {
                this.recursionAllUid(item, initList);
            });
        }

    }

    private List<Integer> threeUid(Integer uid) {
        List<Integer> one = userService.directProxyChain(uid);
        List<Integer> res = new ArrayList<>(one);
        one.forEach(item -> {
            List<Integer> two = userService.directProxyChain(uid);
            res.addAll(two);
            two.forEach(itemTwo -> {
                List<Integer> three = userService.directProxyChain(uid);
                res.addAll(three);
            });
        });
        return res;
    }


}
