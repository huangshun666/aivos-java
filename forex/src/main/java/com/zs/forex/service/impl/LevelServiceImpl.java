package com.zs.forex.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.dto.ProxyInfoDTO;
import com.zs.forex.common.mapper.InvitationRelationMapper;
import com.zs.forex.common.mapper.LevelMapper;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.vcenum.UartType;

import com.zs.forex.common.vcenum.UatStatus;
import com.zs.forex.common.vcenum.UatType;
import com.zs.forex.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LevelServiceImpl extends ServiceImpl<LevelMapper, Level> implements LevelService {


    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserAccountRecordService userAccountRecordService;

    @Autowired
    private UserAccountTradeService userAccountTradeService;
    @Autowired
    private CommissionConfigService commissionConfigService;

    @Autowired
    private RewardRecordService rewardRecordService;

    @Override
    public Level getLevel(Integer uid, boolean add) {

        Long count = rewardRecordService.lambdaQuery().gt(RewardRecord::getUid, 0).eq(RewardRecord::getProxyId, uid).count();

        Optional<Level> max = this.lambdaQuery().orderByAsc(Level::getMark).list().stream()
                .filter(item -> item.getThreshold() <= (add ? count + 1 : count))
                .max(Comparator.comparing(Level::getThreshold));

        return max.orElse(null);
    }


    @Override
    public void rechargeThresholdReward(UserAccountTrade userAccountTrade) {


        User user = userService.pevUser(userAccountTrade.getUid());
        if (user == null)
            return;
        if (rewardRecordService.lambdaQuery().eq(RewardRecord::getUid, userAccountTrade.getUid()).exists())
            return;

        CommissionConfig one = commissionConfigService.lambdaQuery().one();
        BigDecimal efficientThreshold = one.getEfficientThreshold();

        BigDecimal orElse = userAccountRecordService.lambdaQuery().in(UserAccountRecord::getType, UartType.Recharge.ordinal(), UartType.Deposit.ordinal())
                .eq(UserAccountRecord::getUid, userAccountTrade.getUid()).list().stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        if (userAccountTrade.getMoney().add(orElse).compareTo(efficientThreshold) >= 0) {

            Level level = getLevel(user.getId(), true);
            boolean hasLevelAward = false;
            if (level != null) {
                String mark = level.getMark();
                hasLevelAward = rewardRecordService.lambdaQuery().eq(RewardRecord::getProxyId, user.getId())
                        .eq(RewardRecord::getMark, mark).exists();
            }


            BigDecimal def = BigDecimal.valueOf(13);

            Account current = accountService.current(user.getId(), FormulaTool.currency);
            BigDecimal amountAfter, amountBefore;
            amountBefore = current.getBalance();
            amountAfter = amountBefore.add(def);
            current.setBalance(amountAfter);


            UserAccountRecord directPromotionRewardsRecord = new UserAccountRecord();
            directPromotionRewardsRecord.setCtime(new Date());
            directPromotionRewardsRecord.setUid(user.getId());
            directPromotionRewardsRecord.setAid(current.getId());

            directPromotionRewardsRecord.setMoney(def);
            directPromotionRewardsRecord.setBefore(amountBefore);
            directPromotionRewardsRecord.setAfter(amountAfter);
            directPromotionRewardsRecord.setType(UartType.DirectPromotionRewards.ordinal());
            directPromotionRewardsRecord.setRefId(userAccountTrade.getId());

            RewardRecord rewardRecord = new RewardRecord();
            rewardRecord.setUrrId(userAccountTrade.getId());
            rewardRecord.setProxyId(user.getId());
            rewardRecord.setMoney(def);
            rewardRecord.setUid(userAccountTrade.getUid());
            rewardRecord.setCtime(new Date());


            if (!hasLevelAward && level != null) {
                RewardRecord leveAward = new RewardRecord();
                leveAward.setUrrId(userAccountTrade.getId());
                leveAward.setProxyId(user.getId());
                leveAward.setMoney(level.getAward());
                leveAward.setUid(-1);
                leveAward.setCtime(new Date());
                leveAward.setMark(Integer.valueOf(level.getMark()));

                UserAccountRecord leveAwardRecord = new UserAccountRecord();
                leveAwardRecord.setCtime(new Date());
                leveAwardRecord.setUid(user.getId());
                leveAwardRecord.setAid(current.getId());
                leveAwardRecord.setMoney(level.getAward());
                leveAwardRecord.setBefore(current.getBalance());
                leveAwardRecord.setAfter(current.getBalance().add(level.getAward()));
                leveAwardRecord.setType(UartType.LevelAward.ordinal());
                leveAwardRecord.setRefId(userAccountTrade.getId());
                userAccountRecordService.save(leveAwardRecord);
                rewardRecordService.save(leveAward);
                current.setBalance(current.getBalance().add(level.getAward()));
            }

            userAccountRecordService.save(directPromotionRewardsRecord);
            rewardRecordService.save(rewardRecord);
            accountService.changeAccount(current);
        }
    }

    @Override
    public Map<Object, Object> directPushDetails(Integer uid) {
        List<RewardRecord> list = rewardRecordService.lambdaQuery()
                .gt(RewardRecord::getUid, 0).eq(RewardRecord::getProxyId, uid).list();

        BigDecimal total = list.stream().map(RewardRecord::getMoney).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        List<ProxyInfoDTO> infoDTOS = list.stream().map(item -> {
            //账号
            User nextUser = userService.getById(item.getUid());
            //等级
            Level level = this.getLevel(nextUser.getId(), false);
            //团队数量
            List<Integer> coll = userService.directProxyChain(item.getUid());
            int count;
            if (coll.isEmpty())
                count = 0;
            else
                count = userService.lambdaQuery().in(User::getId, coll).count().intValue();
            //充值总金额
            BigDecimal depositTotal = userAccountTradeService.lambdaQuery().eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                    .eq(UserAccountTrade::getType, UatType.Deposit.ordinal())
                    .eq(UserAccountTrade::getUid, item.getUid())
                    .select(UserAccountTrade::getMoney).list().stream().map(UserAccountTrade::getMoney)
                    .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            //直推总收益
            BigDecimal nextTotal = rewardRecordService.lambdaQuery().eq(RewardRecord::getProxyId, item.getUid()).list().stream().map(RewardRecord::getMoney).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            ProxyInfoDTO infoDTO = new ProxyInfoDTO();
            infoDTO.setLevel(level == null ? 0 : Integer.parseInt(level.getMark()));
            infoDTO.setAccount(nextUser.getEmail());
            infoDTO.setGroup(count);
            infoDTO.setDeposit(depositTotal);
            infoDTO.setPl(nextTotal);
            return infoDTO;
        }).collect(Collectors.toList());
        Map<Object, Object> infoMap = new HashMap<>();
        infoMap.put("total", total);
        infoMap.put("info", infoDTOS);
        return infoMap;
    }
}
