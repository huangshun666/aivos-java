package com.zs.forex.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.vcenum.UserRole;

public interface AdminUserService extends IService<AdminUser> {

    /**
     * 后台登录
     *
     * @param token token
     * @return 是否存在
     */
    boolean getAdminByToken(String token);



    /**
     * 后台登录
     *
     * @param token token
     * @return 是否存在
     */
    AdminUser getAdminUserByToken(String token);
    /**
     * 鉴权
     *
     * @param adminUser 当前登录的后台用户
     * @param funRole   方法最小使用角色
     * @return 是否拥有权限
     */
    boolean authentication(AdminUser adminUser, UserRole funRole);

    /**
     * 鉴权
     *
     * @param adminUser 当前登录的后台用户
     * @param funRole   方法最小使用角色
     * @return 是否拥有权限
     */
    boolean authentication(AdminUser adminUser, int funRole);

    /**
     * 后台登录
     *
     * @param userName 账户
     * @param password 密码
     * @return token  如果 token 为null 则登录失败
     */
    String login(String userName, String password, String verifyCode);
}
