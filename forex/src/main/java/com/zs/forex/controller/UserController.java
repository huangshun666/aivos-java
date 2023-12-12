package com.zs.forex.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.aop.NeedLogin;
import com.zs.forex.common.dto.PLDetailsDTO;
import com.zs.forex.common.param.RegisterOrLoginPM;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.vcenum.*;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.common.web.WebException;
import com.zs.forex.service.*;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserAuthService userAuthService;

    private final BankCardService bankCardService;

    private final SendRecordService sendRecordService;

    private final OrderRecordService orderRecordService;

    private final AccountService accountService;

    private final UserAccountRecordService userAccountRecordService;

    private final AdminUserService adminUserService;

    private final UserAccountTradeService userAccountTradeService;


    @PostMapping("/user/register")
    public ResultBody register(@RequestBody RegisterOrLoginPM registerOrLoginPM) throws WebException {
        return ResultBody.success(userService.register(registerOrLoginPM));
    }

    @PostMapping("/user/login")
    public ResultBody login(@RequestBody RegisterOrLoginPM registerOrLoginPM) throws WebException {
        return ResultBody.success(userService.login(registerOrLoginPM));
    }

    @PostMapping("/user/hanTradePwd")
    @NeedLogin
    public ResultBody hanTradePwd() {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        User user = dto.getUser();
        User service = userService.getById(user.getId());
        return ResultBody.success(service.getTradePwd() != null);
    }

    @PostMapping("/user/setTradePwd")
    @NeedLogin
    public ResultBody setTradePwd(@RequestBody JSONObject jsonObject) {
        String tradePwd = jsonObject.getString("tradePwd");

        String loginPwd = jsonObject.getString("loginPwd");

        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        User user = dto.getUser();
        User service = userService.getById(user.getId());
        if (userService.decrypt(service.getPwd()).equals(loginPwd)) {
            user.setTradePwd(userService.encryption(tradePwd));
            userService.updateById(user);
            return ResultBody.success(true);
        }
        return ResultBody.error(RespCodeEnum.old_password_wrong);
    }

    @PostMapping("/user/userAuth")
    @NeedLogin
    public ResultBody userAuth(@RequestBody UserAuth userAuth) {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        if (!userAuthService.lambdaQuery().eq(UserAuth::getUid, dto.getUser().getId())
                .in(UserAuth::getStatus,
                        UatStatus.Passed.ordinal()).exists()) {
            Integer id = dto.getUser().getId();
            UserAuth one = userAuthService.lambdaQuery().eq(UserAuth::getUid, id)
                    .eq(UserAuth::getStatus
                            , UatStatus.Under.ordinal()).last("limit 1").one();
            userAuth.setUid(id);
            userAuth.setCtime(new Date());
            userAuth.setStatus(AuthStatus.No.ordinal());
            if (one != null) {
                userAuth.setId(one.getId());
            }
            userAuthService.saveOrUpdate(userAuth);

        }
        return ResultBody.success(true);
    }

    @PostMapping("/user/bandBankCard")
    @NeedLogin
    public ResultBody bandBankCard(@RequestBody BankCard bankCard) {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        bankCard.setUid(dto.getUser().getId());
        bankCard.setCtime(new Date());
        return ResultBody.success(bankCardService.saveOrUpdate(bankCard));
    }

    @PostMapping("/user/delBankCard")
    @NeedLogin
    public ResultBody delBankCard(@RequestBody BankCard bankCard) {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();

        return ResultBody.success(bankCardService.lambdaUpdate().eq(BankCard::getId, bankCard.getId())
                .eq(BankCard::getUid, dto.getUser().getId()).remove());
    }

    @PostMapping("/user/updateBankCard")
    @NeedLogin
    public ResultBody updateBankCard(@RequestBody BankCard bankCard) {
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.get();
        if (bankCardService.lambdaQuery().eq(BankCard::getUid, dto.getUser().getId())
                .eq(BankCard::getId, bankCard.getId()).exists())
            return ResultBody.success(bankCardService.updateById(bankCard));
        else
            return ResultBody.error(RespCodeEnum.parameter_exception);
    }

    @PostMapping("/user/bankCardList")
    @NeedLogin
    public ResultBody bankCardList(@RequestBody JSONObject jsonObject) {
        Integer id = RequestBodyWeb.get().getUser().getId();
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer type = (Integer) jsonObject.getOrDefault("type", 1);
        return ResultBody.success(bankCardService.lambdaQuery().eq(type != BankCardType.Merchant.ordinal(),
                BankCard::getUid, id
        ).eq(BankCard::getType, type).page(new Page<>(pageIndex, pageSize)).getRecords());
    }


    @PostMapping("/user/sendEmailVerifyCode")
    public ResultBody sendEmailVerifyCode(@RequestBody RegisterOrLoginPM registerOrLoginPM) throws WebException {
        userService.sendEmailVerifyCode(registerOrLoginPM.getEmail(), registerOrLoginPM.getScenes());
        return ResultBody.success();
    }

    @PostMapping("/user/sendCaptchaCode")
    public ResultBody sendCaptchaCode(@RequestBody RegisterOrLoginPM registerOrLoginPM) throws WebException {
        if (ObjectUtil.isAllEmpty(registerOrLoginPM.getArea(), registerOrLoginPM.getPhone(),
                registerOrLoginPM.getScenes()) || ObjectUtil.isAllEmpty(registerOrLoginPM.getEmail(),
                registerOrLoginPM.getScenes())) {
            return ResultBody.error(RespCodeEnum.parameter_exception);
        }

        return ResultBody.success(userService.sendCaptchaCode(registerOrLoginPM.getArea(), registerOrLoginPM.getPhone()
                , registerOrLoginPM.getEmail(), registerOrLoginPM.getScenes()));
    }

    @PostMapping("/user/sendPhoneVerifyCode")
    public ResultBody sendPhoneVerifyCode(@RequestBody RegisterOrLoginPM registerOrLoginPM) throws WebException {
        userService.sendPhoneVerifyCode(registerOrLoginPM.getArea(),
                registerOrLoginPM.getPhone(), registerOrLoginPM.getScenes());
        return ResultBody.success();
    }

    @PostMapping("/user/restPwd")
    public ResultBody restPwd(@RequestBody JSONObject jsonObject) throws WebException {
        String oldCode = jsonObject.getString("oldCode");
        String newCode = jsonObject.getString("newCode");
        String verifyCode = jsonObject.getString("verifyCode");
        String code = jsonObject.getString("code");
        userService.resetPwd(oldCode,
                newCode, verifyCode, code);
        return ResultBody.success();
    }

    @PostMapping("/user/isAuth")
    @NeedLogin
    public ResultBody isAuth() {
        Integer id = RequestBodyWeb.get().getUser().getId();
        return ResultBody.success(userService.isAuth(id));
    }

    /*****************************************admin *****************************************/


    @PostMapping("admin/user/list")
    @AdminNeedLogin
    public ResultBody userList(@RequestBody JSONObject jsonObject) {

        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);

        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);

        Integer userId = jsonObject.getInteger("uid");

        String phone = jsonObject.getString("phone");

        String email = jsonObject.getString("email");

        String relationId = jsonObject.getString("relationId");

        Page<User> page = userService.lambdaQuery().eq(ObjectUtil.isNotEmpty(userId),
                        User::getId, userId)
                .in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                        , User::getId, userService.proxyChain(RequestBodyWeb.get()
                                .getAdminUser().getId()))
                .eq(ObjectUtil.isNotEmpty(phone), User::getPhone, phone)
                .eq(ObjectUtil.isNotEmpty(email), User::getEmail, email)
                .like(relationId != null, User::getRelation, relationId)
                .orderByDesc(User::getCtime).page(new Page<>(pageIndex, pageSize));
        if (page.getRecords() != null) {
            page.getRecords().forEach(item -> {
                String relation = item.getRelation();
                if (relation != null && relation.contains("-")) {
                    String[] split = relation.split("-");
                    List<User> list = userService.lambdaQuery()
                            .in(User::getId, new ArrayList<>(Arrays.asList(split))).select(User::getRelationCode)
                            .orderByAsc(User::getId).list();
                    if (list != null) {
                        String collect = list
                                .stream().filter(f -> f != null && ObjectUtil.isNotEmpty(f.getRelationCode()))
                                .map(User::getRelationCode).collect(Collectors.joining("-"));
                        item.setRelation(collect);
                    }
                }
            });
        }
        return ResultBody.success(page);
    }

    @PostMapping("admin/user/review")
    @AdminNeedLogin(exclude = 3)
    public ResultBody review(@RequestBody UserAuth userAuth) {

        UserAuth data = userAuthService.getById(userAuth.getId());
        if (userAuth.getStatus() == AuthStatus.Yes.ordinal()) {
            User user = userService.getById(data.getUid());
            user.setAuth(AuthStatus.Yes.ordinal());
            user.setName(data.getName());
            userService.updateById(user);
        }
        data.setStatus(userAuth.getStatus());
        return ResultBody.success(userAuthService.updateById(data));
    }

    @PostMapping("admin/user/authList")
    public ResultBody authList(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer userId = jsonObject.getInteger("uid");
        Integer status = jsonObject.getInteger("status");
        return ResultBody.success(userAuthService.lambdaQuery().eq(ObjectUtil.isNotEmpty(userId),
                        UserAuth::getUid, userId)
                .in(RequestBodyWeb.get().getAdminUser().getType() != UserRole.administrator.ordinal()
                        , UserAuth::getUid, userService.proxyChain(RequestBodyWeb.get()
                                .getAdminUser().getId()))
                .eq(ObjectUtil.isNotEmpty(status),
                        UserAuth::getStatus, status).orderByDesc(UserAuth::getCtime).page(new Page<>(pageIndex, pageSize)));
    }

    @PostMapping("admin/user/sendRecord")
    @AdminNeedLogin
    public ResultBody sendRecord(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);

        return ResultBody.success(sendRecordService.lambdaQuery().orderByDesc(SendRecord::getCtime).page(new Page<>(pageIndex, pageSize)));
    }

    @PostMapping("admin/user/allLoginOut")
    @AdminNeedLogin
    public ResultBody allLoginOut() {
        Set<String> keys = stringRedisTemplate.keys("*".concat(CodeScenes.login.getPev()).concat("*"));
        if (keys != null) {
            return ResultBody.success(stringRedisTemplate.delete(keys));
        }
        return ResultBody.success();
    }

    @PostMapping("admin/user/update")
    @AdminNeedLogin
    public ResultBody update(@RequestBody User user) {
        user.setPwd(userService.encryption(user.getPwd()));
        return ResultBody.success(userService.updateById(user));
    }


    @PostMapping("admin/user/register")
    @AdminNeedLogin(exclude = 3)
    public ResultBody adminRegister(@RequestBody RegisterOrLoginPM registerOrLoginPM) {
        return ResultBody.success(userService.registerSuccess(registerOrLoginPM));
    }

    @PostMapping("admin/user/decryptPwd")
    @AdminNeedLogin(exclude = 3)
    public ResultBody decryptPwd(@RequestBody RegisterOrLoginPM registerOrLoginPM) {
        return ResultBody.success(userService.decrypt(registerOrLoginPM.getPwd()));
    }

    @PostMapping("admin/user/pLDetails")
    @AdminNeedLogin
    public ResultBody pLDetails(@RequestBody JSONObject jsonObject) {

        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        Integer userId = jsonObject.getInteger("uid");
        String phone = jsonObject.getString("phone");
        String email = jsonObject.getString("email");

        Page<User> page = userService.lambdaQuery().eq(ObjectUtil.isNotEmpty(userId),
                        User::getId, userId)
                .in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                        , User::getId, userService.proxyChain(RequestBodyWeb.get()
                                .getAdminUser().getId()))
                .eq(ObjectUtil.isNotEmpty(phone), User::getPhone, phone)
                .eq(ObjectUtil.isNotEmpty(email), User::getEmail, email)
                .orderByDesc(User::getCtime).page(new Page<>(pageIndex, pageSize));
        long total = page.getTotal();
        List<PLDetailsDTO> collect = page.getRecords().stream().map(item -> PLDetailsDTO.builder().user(item)
                .balance(accountService.current(item.getId(), FormulaTool.currency).getBalance())
                .sumPL(orderRecordService.lambdaQuery().eq(OrderRecord::getUid, item.getId())
                        .select(OrderRecord::getPl).list().stream().map(OrderRecord::getPl).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO))
                .sumInsertMoney(userAccountRecordService.lambdaQuery()
                        .eq(UserAccountRecord::getUid, item.getId())
                        .in(UserAccountRecord::getType, UartType.Deposit.ordinal(),UartType.Recharge.ordinal())
                        .select(UserAccountRecord::getMoney).list().stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO)
                )
                .build()).collect(Collectors.toList());

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("total", total);
        resMap.put("records", collect);
        return ResultBody.success(resMap);

    }

    @PostMapping("admin/user/adminIndex")
    @AdminNeedLogin
    public ResultBody adminIndex() {
        List<User> list = userService.lambdaQuery().in(RequestBodyWeb.get().getAdminUser().getType() < UserRole.finance.ordinal()
                , User::getId, userService.proxyChain(RequestBodyWeb.get()
                        .getAdminUser().getId())).list();

        list = list.stream().filter(item -> adminUserService.getById(item.getId()) == null).collect(Collectors.toList());

        List<Integer> collect = list.stream().map(User::getId).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            BigDecimal sunPL = orderRecordService.lambdaQuery().in(OrderRecord::getUid, collect)
                    .select(OrderRecord::getPl).list().stream().map(OrderRecord::getPl).reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            BigDecimal sumInsertMoney = userAccountRecordService.lambdaQuery()
                    .in(UserAccountRecord::getUid, collect)
                    .eq(UserAccountRecord::getType, UartType.Recharge.ordinal())
                    .select(UserAccountRecord::getMoney).list().stream().map(UserAccountRecord::getMoney).reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);


            BigDecimal sumOutMoney = userAccountTradeService.lambdaQuery().in(UserAccountTrade::getUid, collect)
                    .eq(UserAccountTrade::getType, UatType.Withdraw.ordinal())
                    .eq(UserAccountTrade::getStatus, UatStatus.Passed.ordinal())
                    .select(UserAccountTrade::getMoney).list().stream().map(UserAccountTrade::getMoney).reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            return ResultBody.success(PLDetailsDTO.builder().sumOutMoney(sumOutMoney).sumInsertMoney(sumInsertMoney).sumPL(sunPL).build());
        }
        return ResultBody.success();
    }


    @PostMapping("admin/user/loginRecordList")
    @AdminNeedLogin
    public ResultBody loginRecordList(@RequestBody JSONObject jsonObject) {
        Integer uid = jsonObject.getInteger("uid");
        String ip = jsonObject.getString(" ");
        Integer pageIndex = jsonObject.getInteger("pageIndex");
        Integer pageSize = jsonObject.getInteger("pageSize");
        return ResultBody.success(userService.getList(uid, ip, pageIndex, pageSize));
    }

}


