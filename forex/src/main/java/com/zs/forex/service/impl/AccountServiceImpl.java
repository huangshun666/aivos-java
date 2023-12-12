package com.zs.forex.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.AccountMapper;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.tools.SnowflakeIdTool;
import com.zs.forex.common.vcenum.UatStatus;
import com.zs.forex.common.vcenum.UatType;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.AccountService;
import com.zs.forex.service.BankCardService;
import com.zs.forex.service.CryptoChainService;
import com.zs.forex.service.UserAccountTradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private CryptoChainService cryptoChainService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserAccountTradeService userAccountTradeService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean userAccountTrade(UserAccountTrade userAccountTrade) throws WebException {
        User user = RequestBodyWeb.get().getUser();
        userAccountTrade.setId(String.valueOf(SnowflakeIdTool.next()));
        userAccountTrade.setStatus(UatStatus.Under.ordinal());
        userAccountTrade.setUid(user.getId());
        userAccountTrade.setCtime(new Date());

        String currency="USD";

        //出金
        if (userAccountTrade.getType().equals(UatType.Withdraw.ordinal())) {
            Account account = this.current(user.getId(), currency);
            if (account.getBalance().compareTo(userAccountTrade.getMoney()) < 0) {
                throw new WebException(RespCodeEnum.insufficient_balance);
            }
            //冻结资金
            account.setBalance(account.getBalance().subtract(userAccountTrade.getMoney()));
            account.setUnbalance(account.getUnbalance().add(userAccountTrade.getMoney()));
            boolean changeAccount = this.changeAccount(account);
            log.info("冻结资产：{}", changeAccount);
        }

        return userAccountTradeService.save(userAccountTrade);
    }


    @Override
    public Account current(Integer uid, String currency) {
        String key = redis_pev.replace("${uid}", uid.toString());

        if (!StringUtils.hasLength(currency)) {
            currency = FormulaTool.currency;
        }

        Account nullAccount = nullAccount(uid, currency);
        Optional<Object> optional = Optional.ofNullable(stringRedisTemplate.opsForHash().get(key, currency));
        boolean present = optional.isPresent();
        if (!present) {
            return nullAccount;
        } else {
            nullAccount = JSONObject.parseObject(optional.get().toString(), Account.class);
        }
        return nullAccount;
    }

    @Override
    public boolean changeAccount(Account account) {
        String key = redis_pev.replace("${uid}", account.getUid().toString());
        account.setBalance(account.getBalance().setScale(2, RoundingMode.HALF_UP));
        account.setUnbalance(account.getUnbalance().setScale(2, RoundingMode.HALF_UP));
        boolean saveOrUpdate = updateById(account);
        stringRedisTemplate.opsForHash().put(key, account.getCurrency(), JSONObject.toJSONString(account));
        return saveOrUpdate;
    }

    @Override
    public void createAccount(Integer uid, String currency) {
        Account current = this.current(uid, currency);
        this.save(current);
        String key = redis_pev.replace("${uid}", uid.toString());
        stringRedisTemplate.opsForHash().put(key, currency, JSONObject.toJSONString(current));

    }

    private Account nullAccount(Integer uid, String currency) {
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);
        account.setUid(uid);
        account.setCurrency(currency);
        account.setUnbalance(BigDecimal.ZERO);
        account.setCtime(new Date());
        return account;
    }
}
