package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.Account;
import com.zs.forex.common.pojo.UserAccountTrade;
import com.zs.forex.common.web.WebException;

import java.util.List;

public interface AccountService extends IService<Account> {

    String redis_pev = "user_account_${uid}";

    /**
     * 出入金记录
     *
     * @param userAccountTrade 记录
     * @return 是否添加成功
     * @throws WebException 参数异常 和 余额不足
     */
    boolean userAccountTrade(UserAccountTrade userAccountTrade) throws WebException;
    /**
     * 获得资产
     *
     * @param uid      用户ID
     * @param currency 货币
     * @return 当前使用的资产
     */
    Account current(Integer uid, String currency);

    /**
     * 资产发送改变
     *
     * @param account 新的资产
     * @return 是否成功
     */
    boolean changeAccount(Account account);


    void createAccount(Integer uid ,String currency);
}
