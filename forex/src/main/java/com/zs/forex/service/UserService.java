package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.dto.UserDTO;
import com.zs.forex.common.param.RegisterOrLoginPM;
import com.zs.forex.common.pojo.LoginRecord;
import com.zs.forex.common.pojo.User;
import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.web.WebException;

import java.util.List;
import java.util.Map;


public interface UserService extends IService<User> {

    String onlyCode = "188888";

    /**
     * 注册
     *
     * @param registerOrLoginPM 用户信息
     * @return 注册成功之后信息
     */
    boolean register(RegisterOrLoginPM registerOrLoginPM) throws WebException;

    /**
     * 登录
     *
     * @param registerOrLoginPM 用户信息
     * @return 登录 成功之后信息
     */
    UserDTO login(RegisterOrLoginPM registerOrLoginPM) throws WebException;

    /**
     * 用户信息
     *
     * @param token token
     * @return 用户
     */
    User getByToken(String token);


    boolean isAuth(Integer uid);


    /**
     * 发送短信码
     *
     * @param area  区号
     * @param phone 手机号
     */
    void sendPhoneVerifyCode(String area, String phone, CodeScenes scenes) throws WebException;

    /**
     * 发送短图片验证码
     *
     * @param area  区号
     * @param phone 手机号
     */
    Map<String, Object> sendCaptchaCode(String area, String phone, String email, CodeScenes scenes) throws WebException;

    /**
     * 发送注册邮箱码
     *
     * @param email 邮箱号
     */
    void sendEmailVerifyCode(String email, CodeScenes scenes) throws WebException;

    /**
     * 验证场景
     *
     * @param scenes     场景列表
     * @param code       key
     * @param verifyCode 验证码
     * @return 是否通过
     */
    void verifyCode(CodeScenes scenes, String code, String verifyCode) throws WebException;

    /**
     * 加密 密码
     *
     * @param pwd 密码
     * @return 密文
     */
    String encryption(String pwd);

    /**
     * 解密 密码
     *
     * @param pwd 密码
     * @return 明文
     */
    String decrypt(String pwd);

    /**
     * 系统唯一code
     *
     * @param initCode 初始值
     * @return 当前的code码
     */
    String onlyCode(String initCode, boolean get);

    /**
     * 重置密码
     *
     * @param oldCode 旧密码
     * @param newCode 新密码
     * @param verify  验证码
     * @param code
     */
    void resetPwd(String oldCode, String newCode, String verify, String code) throws WebException;

    /**
     * 登录成功回调
     *
     * @param user 用户信息
     * @return 新的用户信息
     */
    UserDTO success(User user);


    /**
     * 注册成功回调
     *
     * @return 新的用户信息
     */
    boolean registerSuccess(RegisterOrLoginPM registerOrLoginPM);

    /**
     * 后管 代理链
     *
     * @param proxyId 代理id
     * @return
     */
    List<Integer> proxyChain(Integer proxyId);

    /**
     * 直推类用
     * @param proxyId 代理id
     * @return
     */
    List<Integer>directProxyChain(Integer proxyId);

    /**
     * 下级 id
     * @param nextUid
     * @return
     */
    User pevUser(Integer nextUid);


    Page<LoginRecord> getList(Integer uid,String  ip,Integer pageIndex,Integer pageSize);

}
