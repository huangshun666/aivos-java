package com.zs.forex.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.aop.NeedLogin;
import com.zs.forex.common.dto.AccountRecordListDTO;
import com.zs.forex.common.dto.AdminAccountDTO;
import com.zs.forex.common.dto.ChuJinDTO;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.tools.SnowflakeIdTool;
import com.zs.forex.common.vcenum.UartType;
import com.zs.forex.common.vcenum.UatStatus;
import com.zs.forex.common.vcenum.UatType;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.*;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
public class AccountController {


    private final AccountService accountService;

    private final CryptoChainService cryptoChainService;
    private final UserService userService;

    private final UserAccountTradeService userAccountTradeService;
    private final BankCardService bankCardService;

    private final UserAccountRecordService userAccountRecordService;

    private final LevelService levelService;

    @PostMapping("/account/current")
    @NeedLogin
    public ResultBody current() {
        return ResultBody.success(accountService.current(RequestBodyWeb.get().getUser().getId(), "USD"));
    }


    @PostMapping("/account/record")
    @NeedLogin
    public ResultBody current(@RequestBody JSONObject jsonObject) {
        User user = RequestBodyWeb.get().getUser();
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        return ResultBody.success(userAccountRecordService.lambdaQuery().orderByDesc(UserAccountRecord::getCtime)
                .eq(UserAccountRecord::getUid, user.getId()).page(new Page<>(pageIndex, pageSize)).getRecords());
    }

    @PostMapping("/account/ruJin")
    @NeedLogin
    public ResultBody ruJin(@RequestBody JSONObject jsonObject) throws WebException {
        User user = RequestBodyWeb.get().getUser();
        UserAccountTrade userAccountTrade = jsonObject.toJavaObject(UserAccountTrade.class);
        if (userAccountTrade.getType() == UatType.Withdraw.ordinal()) {
            String tradPwd = jsonObject.getString("tradPwd");
            if (!userService.getById(user.getId()).getTradePwd()
                    .equals(userService.encryption(tradPwd))) {
                return ResultBody.error(RespCodeEnum.trade_pwd);
            }
        } else {
            boolean exists = userAccountTradeService.lambdaQuery().eq(UserAccountTrade::getType, UatType.Deposit.ordinal())
                    .eq(UserAccountTrade::getUid, user.getId())
                    .eq(UserAccountTrade::getStatus, UatStatus.Under.ordinal()).exists();
            if (exists) {
                return ResultBody.error(RespCodeEnum.repeat_submit);
            }
        }
        return ResultBody.success(accountService.userAccountTrade(userAccountTrade));
    }

    @PostMapping("/account/ruJinList")
    @NeedLogin
    public ResultBody ruJinList(@RequestBody JSONObject jsonObject) {
        User user = RequestBodyWeb.get().getUser();
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer type = (Integer) jsonObject.getOrDefault("type", 1);
        List<ChuJinDTO> chuJinDTOS = userAccountTradeService.lambdaQuery()
                .eq(UserAccountTrade::getUid, user.getId())
                .eq(UserAccountTrade::getType, type).page(new Page<>(pageIndex, pageSize)).getRecords().stream().map(
                        item -> {
                            ChuJinDTO dto = new ChuJinDTO();
                            dto.setUserAccountTrade(item);
                            if (item.getTradeType() == 0)
                                dto.setBankCard(bankCardService.getById(item.getBankCardId()));
                            else
                                dto.setBankCard(cryptoChainService.getById(item.getBankCardId()));
                            return dto;
                        }
                ).collect(Collectors.toList());
        return ResultBody.success(chuJinDTOS);
    }

    /****************************************后台**************************************/

    @PostMapping("admin/account/list")
    @AdminNeedLogin
    public ResultBody adminAccountList(@RequestBody JSONObject jsonObject) throws WebException {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer uid = jsonObject.getInteger("uid");
        String phone = jsonObject.getString("phone");
        String email = jsonObject.getString("email");
        if (StringUtils.hasLength(phone)) {
            User one = userService.lambdaQuery().eq(User::getPhone, phone).last("limit 1").one();
            uid = one.getId();
        }
        if (StringUtils.hasLength(email)) {
            User one = userService.lambdaQuery().eq(User::getEmail, email).last("limit 1").one();
            uid = one.getId();
        }
        Page<Account> page = accountService.lambdaQuery()
                .eq(uid != null, Account::getUid, uid)
                .in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                        , Account::getUid, userService.proxyChain(RequestBodyWeb.get().getAdminUser().getId()))
                .page(new Page<>(pageIndex, pageSize));

        List<AdminAccountDTO> collect = page.getRecords().stream().map(item -> {
            AdminAccountDTO dto = new AdminAccountDTO();
            dto.setAccount(item);
            dto.setUser(userService.getById(item.getUid()));
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("records", collect);
        objectMap.put("total", page.getTotal());

        return ResultBody.success(objectMap);
    }

    @PostMapping("admin/account/recharge")
    @AdminNeedLogin(minType = UserRole.finance)
    public ResultBody adminAccountRecharge(@RequestBody JSONObject jsonObject)
            throws WebException {
        Integer id = jsonObject.getInteger("id");
        BigDecimal money = jsonObject.getBigDecimal("money");
        Integer type = jsonObject.getInteger("type");
        String remark = jsonObject.getString("remark");
        if (!ObjectUtil.isAllNotEmpty(id, type, money)) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        Account service = accountService.getById(id);
        Account account = accountService.current(service.getUid(), FormulaTool.currency);
        //流水记录
        BigDecimal before = account.getBalance();
        BigDecimal after = before.add(money);
        UserAccountRecord userAccountRecord = new UserAccountRecord();
        userAccountRecord.setType(type);
        userAccountRecord.setAfter(after);
        userAccountRecord.setBefore(before);
        userAccountRecord.setRefId("-1");
        userAccountRecord.setAid(account.getId());
        userAccountRecord.setMoney(money);
        userAccountRecord.setUid(account.getUid());
        userAccountRecord.setCtime(new Date());
        userAccountRecord.setRemark(remark);
        userAccountRecordService.save(userAccountRecord);
        account.setBalance(after);
        accountService.changeAccount(account);
        if (type == UartType.Recharge.ordinal()) {
            UserAccountTrade userAccountTrade = new UserAccountTrade();
            userAccountTrade.setMoney(money);
            userAccountTrade.setUid(userAccountRecord.getUid());
            userAccountTrade.setId(String.valueOf(SnowflakeIdTool.next()));
            levelService.rechargeThresholdReward(userAccountTrade);
        }
        return ResultBody.success();
    }

    @PostMapping("admin/account/bankCardList")
    @AdminNeedLogin
    public ResultBody bankCardList(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer uid = jsonObject.getInteger("uid");
        Integer type = jsonObject.getInteger("type");
        return ResultBody.success(bankCardService.lambdaQuery().eq(uid != null,
                        BankCard::getUid, uid)
                .eq(type != null, BankCard::getType, type)
                .in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                        , BankCard::getUid, userService.proxyChain(RequestBodyWeb.get().getAdminUser().getId()))
                .page(new Page<>(pageIndex, pageSize)));

    }

    @PostMapping("admin/account/bankCardAddOrUpdate")
    @AdminNeedLogin(exclude = 3)
    public ResultBody bankCardAddOrUpdate(@RequestBody BankCard bankCard) {
        bankCard.setCtime(new Date());
        return ResultBody.success(bankCardService.saveOrUpdate(bankCard));

    }

    @PostMapping("admin/account/review")
    @AdminNeedLogin(exclude = 3)
    public ResultBody review(@RequestBody JSONObject jsonObject) throws WebException {
        String id = jsonObject.getString("id");
        Integer type = jsonObject.getInteger("type");
        if (ObjectUtil.isAllEmpty(id, type)) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }
        userAccountTradeService.review(id, type);
        return ResultBody.success();
    }

    @PostMapping("admin/account/ruJinList")
    @AdminNeedLogin
    public ResultBody adminRuJinList(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer type = jsonObject.getInteger("type");
        Integer uid = jsonObject.getInteger("uid");
        Integer status = jsonObject.getInteger("status");
        String phone = jsonObject.getString("phone");
        String email = jsonObject.getString("email");
        if (StringUtils.hasLength(phone)) {
            User one = userService.lambdaQuery().eq(User::getPhone, phone).last("limit 1").one();
            uid = one.getId();
        }
        if (StringUtils.hasLength(email)) {
            User one = userService.lambdaQuery().eq(User::getEmail, email).last("limit 1").one();
            uid = one.getId();
        }
        Page<UserAccountTrade> page = userAccountTradeService.lambdaQuery()
                .eq(uid != null, UserAccountTrade::getUid, uid)
                .eq(type != null, UserAccountTrade::getType, type)
                .eq(status != null, UserAccountTrade::getStatus, status)
                .in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                        , UserAccountTrade::getUid, userService.proxyChain(RequestBodyWeb.get()
                                .getAdminUser().getId()))
                .orderByDesc(UserAccountTrade::getCtime)
                .page(new Page<>(pageIndex, pageSize));

        List<ChuJinDTO> chuJinDTOS = page.getRecords().stream().map(
                item -> {
                    ChuJinDTO dto = new ChuJinDTO();
                    dto.setUserAccountTrade(item);
                    if (item.getTradeType() == 0) {
                        BankCard byId = bankCardService.getById(item.getBankCardId());
                        dto.setBankCard(byId);
                    } else {
                        CryptoChain chain = cryptoChainService.getById(item.getBankCardId());
                        dto.setBankCard(chain);
                    }
                    dto.setUser(userService.getById(item.getUid()));
                    return dto;
                }
        ).collect(Collectors.toList());
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("total", page.getTotal());
        resMap.put("records", chuJinDTOS);
        return ResultBody.success(resMap);
    }


    @PostMapping("admin/account/adminUserAccountRecordList")
    @AdminNeedLogin
    public ResultBody adminUserAccountRecordList(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer uid = jsonObject.getInteger("uid");
        String phone = jsonObject.getString("phone");
        String email = jsonObject.getString("email");
        String startTime = jsonObject.getString("startTime");
        String endTime = jsonObject.getString("endTime");

        if (StringUtils.hasLength(phone)) {
            User one = userService.lambdaQuery().eq(User::getPhone, phone).last("limit 1").one();
            uid = one.getId();
        }
        Integer type = jsonObject.getInteger("type");
        if (StringUtils.hasLength(email)) {
            User one = userService.lambdaQuery().eq(User::getEmail, email).last("limit 1").one();
            uid = one.getId();
        }
        Page<UserAccountRecord> page = userAccountRecordService.lambdaQuery()
                .eq(uid != null, UserAccountRecord::getUid, uid)
                .eq(type != null, UserAccountRecord::getType, type)
                .in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                        , UserAccountRecord::getUid, userService.proxyChain(RequestBodyWeb.get()
                                .getAdminUser().getId()))
                .orderByDesc(UserAccountRecord::getCtime)
                .between(ObjectUtil.isAllNotEmpty(startTime, endTime), UserAccountRecord::getCtime, startTime, endTime)
                .page(new Page<>(pageIndex, pageSize));

        List<AccountRecordListDTO> list = page.getRecords().stream().map(item -> {
            Integer id = item.getUid();
            User byId = userService.getById(id);
            AccountRecordListDTO accountRecordListDTO = new AccountRecordListDTO();
            accountRecordListDTO.setUserAccountRecord(item);
            accountRecordListDTO.setUser(byId);
            return accountRecordListDTO;
        }).collect(Collectors.toList());
        Page<AccountRecordListDTO> accountRecordListDTOPage = new Page<>();
        accountRecordListDTOPage.setRecords(list);
        accountRecordListDTOPage.setTotal(page.getTotal());
        return ResultBody.success(accountRecordListDTOPage);
    }

}
