package com.zs.forex.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.OnlineWithdrawRecordMapper;
import com.zs.forex.common.mapper.UserAccountTradeMapper;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.vcenum.*;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserAccountTradeServiceImpl extends ServiceImpl<UserAccountTradeMapper, UserAccountTrade>
        implements UserAccountTradeService {

    @Autowired
    @Lazy
    private AccountService accountService;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private CryptoChainService cryptoChainService;

    @Autowired
    private UserAccountRecordService userAccountRecordService;
    @Lazy
    @Autowired
    private LevelService levelService;


    @Autowired
    private OnlineWithdrawRecordService onlineWithdrawRecordService;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void review(String id, Integer type) throws WebException {
        UserAccountTrade accountTrade = this.getById(id);
        if (accountTrade == null || accountTrade.getStatus() != UatStatus.Under.ordinal())
            throw new WebException(RespCodeEnum.parameter_exception);

        UserAccountRecord userAccountRecord = new UserAccountRecord();
        userAccountRecord.setCtime(new Date());
        userAccountRecord.setUid(accountTrade.getUid());
        Integer bankCardId = accountTrade.getBankCardId();
        Account account = accountService.current(accountTrade.getUid(), "USD");
        CryptoChain chain = cryptoChainService.getById(bankCardId);
        if (accountTrade.getType() == UatType.Deposit.ordinal()
                && (accountTrade.getTradeType() == 0 ? bankCardService.getById(bankCardId).getType() == BankCardType.Merchant.ordinal()
                : chain.getUid() == BankCardType.Merchant.ordinal())
        ) {
            if (AuthStatus.Yes.ordinal() == type) {
                BigDecimal before = account.getBalance();
                BigDecimal after = before.add(accountTrade.getMoney());
                account.setBalance(after);
                userAccountRecord.setBefore(before);
                userAccountRecord.setAfter(after);
                userAccountRecord.setMoney(accountTrade.getMoney());
                userAccountRecord.setType(UartType.Deposit.ordinal());
                accountTrade.setStatus(UatStatus.Passed.ordinal());
                userAccountRecord.setAid(account.getId());
                userAccountRecord.setRefId(accountTrade.getId());
                userAccountRecordService.save(userAccountRecord);


            } else {
                accountTrade.setStatus(UatStatus.Fail.ordinal());
            }
        } else if (accountTrade.getType() == UatType.Withdraw.ordinal()) {
            if (AuthStatus.Yes.ordinal() == type) {
                BigDecimal before = account.getBalance();
                BigDecimal unBefore = account.getUnbalance();
                account.setUnbalance(unBefore.subtract(accountTrade.getMoney()));
                userAccountRecord.setAfter(before);
                userAccountRecord.setBefore(before.add(accountTrade.getMoney()));
                userAccountRecord.setMoney(accountTrade.getMoney().negate());
                userAccountRecord.setType(UartType.Withdraw.ordinal());
                accountTrade.setStatus(UatStatus.Passed.ordinal());
                userAccountRecord.setAid(account.getId());
                userAccountRecord.setRefId(accountTrade.getId());
                userAccountRecordService.save(userAccountRecord);
                //发送打款申请
                onlineWithdrawRecordService.execWithdraw(chain.getAddress(), accountTrade.getUid(), chain.getChain(), accountTrade.getMoney(), accountTrade.getId());
            } else {
                account.setUnbalance(account.getUnbalance().subtract(accountTrade.getMoney()));
                account.setBalance(account.getBalance().add(accountTrade.getMoney()));
                accountTrade.setStatus(UatStatus.Fail.ordinal());
            }
        } else {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        accountService.changeAccount(account);
        accountTrade.setMtime(new Date());
        this.updateById(accountTrade);
        if (accountTrade.getType() == UatType.Deposit.ordinal()) {
            levelService.rechargeThresholdReward(accountTrade);
        }
    }
}
