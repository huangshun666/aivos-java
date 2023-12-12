package com.zs.forex.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.dto.UserDTO;
import com.zs.forex.common.mapper.*;
import com.zs.forex.common.param.RegisterOrLoginPM;
import com.zs.forex.common.pojo.*;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.tools.RestTool;
import com.zs.forex.common.vcenum.AuthStatus;
import com.zs.forex.common.vcenum.BlackStatus;
import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.common.web.I18nMessageUtil;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.WebException;
import com.zs.forex.handler.CoreHandler;
import com.zs.forex.service.AccountService;
import com.zs.forex.service.AdminUserService;
import com.zs.forex.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final StringRedisTemplate stringRedisTemplate;

    private final JavaMailSender javaMailSender;

    private final SendRecordMapper sendRecordMapper;

    private final AdminUserMapper adminUserMapper;

    @Autowired
    @Lazy
    private AccountService accountService;


    @Autowired
    private InvitationRelationMapper invitationRelationMapper;

    @Autowired
    private LoginRecordMapper loginRecordMapper;


    @Transactional
    @Override
    public boolean register(RegisterOrLoginPM registerOrLoginPM) throws WebException {

        if (ObjectUtil.isAllEmpty(registerOrLoginPM.getEmail(), registerOrLoginPM.getPhone(), registerOrLoginPM.getPwd())) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }

        if (ObjectUtil.isNotEmpty(registerOrLoginPM.getEmail())) {
            if (!isEmail(registerOrLoginPM.getEmail())) {
                throw new WebException(RespCodeEnum.mail_error);
            }
            if (this.lambdaQuery().eq(User::getEmail, registerOrLoginPM.getEmail()).exists()) {
                throw new WebException(RespCodeEnum.mail_exist);
            }
            verifyCode(CodeScenes.register, registerOrLoginPM.getEmail(), registerOrLoginPM.getVerifyCode());

        }

        if (ObjectUtil.isAllNotEmpty(registerOrLoginPM.getPhone(), registerOrLoginPM.getArea())) {
            if (!isMobileNumber(registerOrLoginPM.getPhone())) throw new WebException(RespCodeEnum.phone_error);

            if (this.lambdaQuery().eq(User::getPhone, registerOrLoginPM.getPhone()).eq(User::getArea, registerOrLoginPM.getArea()).exists()) {
                throw new WebException(RespCodeEnum.phone_exist);
            }

            verifyCode(CodeScenes.register, registerOrLoginPM.getArea().concat(registerOrLoginPM.getPhone()), registerOrLoginPM.getVerifyCode());

        }


        return this.registerSuccess(registerOrLoginPM);
    }


    @Override
    public UserDTO login(RegisterOrLoginPM registerOrLoginPM) throws WebException {


        User user;
        if (ObjectUtil.isAllEmpty(registerOrLoginPM.getEmail(), registerOrLoginPM.getVerifyCode(), registerOrLoginPM.getPhone(), registerOrLoginPM.getPwd())) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }

        if (ObjectUtil.isAllNotEmpty(registerOrLoginPM.getEmail(), registerOrLoginPM.getPwd())) {

            verifyCode(CodeScenes.login, registerOrLoginPM.getEmail(), registerOrLoginPM.getVerifyCode());

            user = this.lambdaQuery().eq(User::getEmail, registerOrLoginPM.getEmail())
                    .eq(User::getPwd, this.encryption(registerOrLoginPM.getPwd()))
                    .last("limit 1").one();

            if (user != null) {
                if (user.getOnline() == BlackStatus.Yes.ordinal()) {
                    throw new WebException(RespCodeEnum.account_locked);
                }
                return this.success(user);
            }
        }

        if (ObjectUtil.isAllNotEmpty(registerOrLoginPM.getPhone(), registerOrLoginPM.getArea(), registerOrLoginPM.getPwd())) {
            verifyCode(CodeScenes.login, registerOrLoginPM.getArea().concat(registerOrLoginPM.getPhone()), registerOrLoginPM.getVerifyCode());
            user = this.lambdaQuery().eq(User::getPhone, registerOrLoginPM.getPhone()).eq(User::getArea, registerOrLoginPM.getArea()).eq(User::getPwd, this.encryption(registerOrLoginPM.getPwd())).last("limit 1").one();
            if (user != null) {

                return this.success(user);
            }
        }

        throw new WebException(RespCodeEnum.auth_error);
    }

    @Override
    public User getByToken(String token) {
        String userInfo = stringRedisTemplate.opsForValue().get(CodeScenes.login.getPev().concat(token));
        if (userInfo == null) return null;
        return JSONObject.parseObject(userInfo, User.class);
    }

    @Override
    public boolean isAuth(Integer uid) {
        User user = this.getById(uid);
        return user != null && user.getAuth() == AuthStatus.Yes.ordinal();
    }

    @Override
    public void sendPhoneVerifyCode(String area, String phone, CodeScenes scenes) throws WebException {
        if (!isMobileNumber(phone)) {
            throw new WebException(RespCodeEnum.phone_error);
        }
        if (scenes != CodeScenes.register) {
            if (!this.lambdaQuery().eq(User::getPhone, phone).eq(User::getArea, area).exists()) {
                throw new WebException(RespCodeEnum.phone_not_exist);
            }
        }
        String lang = RequestBodyWeb.get().getLang();
        Runnable r = () -> {
            String key = scenes.getPev().concat(area.concat(phone));
            String content = I18nMessageUtil.getMessage(lang, scenes.getCode(), "");
            String verifyCode = generateVerifyCode();
            String concat = content.concat(": ").concat(verifyCode);
            RestTool.builder().url("http://api.wftqm.com/api/sms/mtsend").addParam("appkey", "7f1l7cVW").addParam("secretkey", "EmfW8PiJ").addParam("phone", area.concat(phone)).addParam("content", concat).post(false).sync();
            SendRecord sendRecord = new SendRecord();
            sendRecord.setTo(area.concat(phone));
            sendRecord.setCtime(new Date());
            sendRecord.setType(Integer.valueOf(CodeScenes.phone.getCode()));
            sendRecord.setContent(concat);
            sendRecordMapper.insert(sendRecord);
            stringRedisTemplate.opsForValue().set(key, verifyCode, Duration.ofMinutes(3));
            log.info("send PhoneVerifyCode success：{} {}", JSONObject.toJSONString(sendRecord), scenes);
        };

        CoreHandler.addTask(r);

    }

    @Override
    public Map<String, Object> sendCaptchaCode(String area, String phone, String email, CodeScenes scenes) {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(250, 100);
//        String key = isEmail(email) ? scenes.getPev().concat(email) :
//                scenes.getPev().concat(area.concat(phone));
//        stringRedisTemplate.opsForValue().set(key, lineCaptcha.getCode(), Duration.ofMinutes(3));
//        log.info("send sendCaptchaCode success {}", lineCaptcha.getCode());
        Map<String, Object> res = new HashMap<>();
        res.put("captcha", lineCaptcha.getImageBase64Data());
        res.put("code", lineCaptcha.getCode());
        return res;
    }

    @Transactional
    @Override
    public void sendEmailVerifyCode(String email, CodeScenes scenes) throws WebException {
        if (!isEmail(email)) {
            throw new WebException(RespCodeEnum.mail_error);
        }
        if (scenes != CodeScenes.register) {
            if (!this.lambdaQuery().eq(User::getEmail, email).exists()) {
                throw new WebException(RespCodeEnum.mail_not_exist);
            }
        }
        String lang = RequestBodyWeb.get().getLang();
        Runnable r = () -> {
            String key = scenes.getPev().concat(email);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("surpportercenter@gmail.com");
            String content = I18nMessageUtil.getMessage(lang, scenes.getCode(), "");
            message.setSubject(content);
            String verifyCode = this.generateVerifyCode();
            String concat = content.concat(": ").concat(verifyCode);
            message.setText(concat);
            message.setTo(email);
            javaMailSender.send(message);
            SendRecord sendRecord = new SendRecord();
            sendRecord.setTo(email);
            sendRecord.setCtime(new Date());
            sendRecord.setType(Integer.valueOf(CodeScenes.email.getCode()));
            sendRecord.setContent(concat);
            sendRecordMapper.insert(sendRecord);
            stringRedisTemplate.opsForValue().set(key, verifyCode, Duration.ofMinutes(3));
            log.info("send EmailVerifyCode success：{}", JSONObject.toJSONString(sendRecord));
        };

        CoreHandler.addTask(r);
    }

    @Override
    public void verifyCode(CodeScenes scenes, String code, String verifyCode) throws WebException {
//        String key = scenes.getPev().concat(code);
//        boolean exist = Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
//        if (!exist) {
//            throw new WebException(RespCodeEnum.verification_used_or_expired);
//        }
//        boolean equals = Objects.equals(stringRedisTemplate.opsForValue().get(key), verifyCode);
//        if (!equals) {
//            throw new WebException(RespCodeEnum.verification_code_error);
//        }
//
//
//        stringRedisTemplate.delete(key);
    }

    @Override
    public String encryption(String pwd) {
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, Base64.getDecoder().decode(CodeScenes.key.getPev()));
        return Base64.getEncoder().encodeToString(aes.encrypt(pwd));
    }

    public static void main(String[] args) {
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, Base64.getDecoder().decode(CodeScenes.key.getPev()));
        System.out.println(Base64.getEncoder().encodeToString(aes.encrypt("123123")));
        ;
    }

    @Override
    public String decrypt(String pwd) {
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, Base64.getDecoder().decode(CodeScenes.key.getPev()));
        byte[] decode = Base64.getDecoder().decode(pwd);
        return new String(aes.decrypt(decode), StandardCharsets.UTF_8);
    }

    @Override
    public String onlyCode(String initCode, boolean get) {
        String onlyCode = "onlyCode";
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(onlyCode))) {
            if (get) {
                return Objects.requireNonNull(stringRedisTemplate.opsForValue().increment(onlyCode)).toString();
            }
            return stringRedisTemplate.opsForValue().get(onlyCode);
        } else {
            stringRedisTemplate.opsForValue().set(onlyCode, initCode);
            return initCode;
        }
    }

    @Override
    public void resetPwd(String oldCode, String newCode, String verify, String code) throws WebException {

        RequestBodyWeb.RequestBodyDTO requestBodyDTO = RequestBodyWeb.get();
        User user = requestBodyDTO.getUser() == null ? this.lambdaQuery()
                .eq(User::getEmail, code).or().eq(User::getPhone, code).or().
                eq(User::getId, code).list().get(0) :
                requestBodyDTO.getUser();
        if (!ObjectUtil.isAllNotEmpty(oldCode, newCode, user)) {
            throw new WebException(RespCodeEnum.parameter_exception);
        }

        if (!this.getById(user.getId()).getPwd().equals(this.encryption(oldCode))) {
            throw new WebException(RespCodeEnum.old_password_wrong);
        }
        String loginCode = !StringUtils.hasLength(user.getPhone()) ? user.getEmail() : user.getArea().concat(user.getPhone());

        verifyCode(CodeScenes.reset_pwd, loginCode, verify);

        user.setPwd(encryption(newCode));
        this.updateById(user);
        stringRedisTemplate.delete(CodeScenes.login.getPev().concat(requestBodyDTO.getToken()));
    }

    //验证手机号码
    private boolean isMobileNumber(String mobileNumber) {
        if (mobileNumber.length() != 11) {
            return false;
        }
        char c = mobileNumber.charAt(1);
        if (c < '3' || c > '9') {
            return false;
        }
        for (int i = 2; i < 11; i++) {
            c = mobileNumber.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    //验证邮箱
    private boolean isEmail(String email) {
        if (email == null || email.trim().length() == 0) {
            return false;
        }
        String regex = "^([a-z\\dA-Z]+[-|.]?)+[a-z\\dA-Z]@([a-z\\dA-Z]+(-[a-z\\dA-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    //生成6位码
    private String generateVerifyCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int n = (int) (Math.random() * 10); // 0-9的随机数
            code.append(n);
        }
        return code.toString();
    }

    //处理邀请关系
    private String handleRelation(String id, String relationCode) {
        User pUser = this.lambdaQuery().eq(User::getRelationCode, relationCode).last("limit 1").one();
        if (pUser == null) return null;
        String relation = pUser.getRelation();
        return relation == null ? pUser.getId().toString().concat("-").concat(id).concat("-") : relation.concat(id).concat("-");
    }

    public UserDTO success(User user) {
        RequestBodyWeb.RequestBodyDTO requestBodyDTO = RequestBodyWeb.get();
        String token = IdUtil.simpleUUID();
        stringRedisTemplate.opsForValue().set(CodeScenes.login.getPev().concat(token), JSONObject.toJSONString(user), Duration.ofDays(7 * 2));
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setToken(token);
        dto.setUid(user.getId());

        //记录登录记录
        LoginRecord loginRecord = new LoginRecord();
        loginRecord.setDevice(requestBodyDTO.getDevice());
        loginRecord.setDeviceNo(requestBodyDTO.getDeviceNo());
        loginRecord.setIp(requestBodyDTO.getIp());
        loginRecord.setUid(user.getId());
        loginRecord.setCtime(new Date());
        loginRecordMapper.insert(loginRecord);

        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean registerSuccess(RegisterOrLoginPM registerOrLoginPM) {
        AdminUser adminUser = RequestBodyWeb.get().getAdminUser();
        if (adminUser != null) { //带有身份
            Integer id = adminUser.getId();
            User byId = this.getById(id);
            String relationCode = byId == null ? null : byId.getRelationCode();
            registerOrLoginPM.setRelationCode(relationCode);
        }
        User user = new User();
        user.setCtime(new Date());
        user.setAuth(AuthStatus.No.ordinal());
        user.setArea(registerOrLoginPM.getArea());
        user.setEmail(registerOrLoginPM.getEmail());
        user.setOnline(AuthStatus.No.ordinal());
        user.setBlack(BlackStatus.No.ordinal());
        user.setPhone(registerOrLoginPM.getPhone());
        user.setPwd(this.encryption(registerOrLoginPM.getPwd()));
        user.setRelationCode(this.onlyCode(null, true));
        user.setMtime(new Date());
        user.setName(registerOrLoginPM.getName());
        boolean save = this.save(user);
        if (save) {
            if (ObjectUtil.isAllNotEmpty(registerOrLoginPM.getRelationCode())) {
                String handleRelation = this.handleRelation(user.getId().toString(), registerOrLoginPM.getRelationCode());
                user.setRelation(handleRelation);
                boolean b = this.updateById(user);
                //添加邀请记录
                User pUser = this.lambdaQuery().eq(User::getRelationCode, registerOrLoginPM.getRelationCode()).last("limit 1").one();
                if (pUser != null) {
                    InvitationRelation invitationRelation = new InvitationRelation();
                    invitationRelation.setInvitationId(pUser.getId());
                    invitationRelation.setCtime(new Date());
                    invitationRelation.setUid(user.getId());
                    int insert = invitationRelationMapper.insert(invitationRelation);
                    log.info("添加邀请记录 {}", insert);
                }

            }
            //创建资产
            accountService.createAccount(user.getId(), FormulaTool.currency);

            return true;
        }
        return false;
    }

    @Override
    public List<Integer> proxyChain(Integer proxyId) {
        AdminUser adminUser = adminUserMapper.selectById(proxyId);
        if (adminUser != null && adminUser.getType() == UserRole.administrator.ordinal()) {
            List<Integer> list = this.lambdaQuery().select(User::getId).list().stream().map(User::getId).
                    collect(Collectors.toList());
            return list.isEmpty() ? Collections.singletonList(-1) : list;
        }
        List<Integer> collect = this.lambdaQuery().select(User::getId).like(User::getRelation, proxyId).list()
                .stream().map(User::getId).collect(Collectors.toList());
        return collect.isEmpty() ? Collections.singletonList(-1) : collect;
    }

    @Override
    public List<Integer> directProxyChain(Integer proxyId) {
        return invitationRelationMapper
                .selectList(new LambdaQueryWrapper<InvitationRelation>()
                        .eq(InvitationRelation::getInvitationId, proxyId)).stream()
                .map(InvitationRelation::getUid).collect(Collectors.toList());


    }

    @Override
    public User pevUser(Integer nextUid) {
        InvitationRelation relation = invitationRelationMapper.selectOne(new LambdaQueryWrapper<InvitationRelation>()
                .eq(InvitationRelation::getUid, nextUid).last("limit 1"));
        if (relation == null)
            return null;
        return this.getById(relation.getInvitationId());
    }

    @Override
    public Page<LoginRecord> getList(Integer uid, String ip, Integer pageIndex, Integer pageSize) {
        return loginRecordMapper.selectPage(new Page<LoginRecord>(pageIndex, pageSize)
                , new LambdaQueryWrapper<LoginRecord>()
                        .eq(uid != null, LoginRecord::getUid, uid)
                        .eq(StringUtils.hasText(ip), LoginRecord::getIp, ip
                        ));
    }
}
