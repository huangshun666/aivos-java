package com.zs.forex.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.forex.common.mapper.AdminUserMapper;
import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.service.AdminUserService;
import com.zs.forex.service.UserService;
import dev.samstevens.totp.code.CodeVerifier;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@AllArgsConstructor
@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {
    private final StringRedisTemplate stringRedisTemplate;

    private final CodeVerifier codeVerifier;

    private  UserService userService;

    @Override
    public boolean getAdminByToken(String token) {
        String userInfo = stringRedisTemplate.opsForValue().get(CodeScenes.admin_login.getPev().concat(token));
        return userInfo != null;
    }

    @Override
    public AdminUser getAdminUserByToken(String token) {
        String userInfo = stringRedisTemplate.opsForValue().get(CodeScenes.admin_login.getPev().concat(token));
        if (userInfo != null) {
            return JSONObject.parseObject(userInfo, AdminUser.class);
        }
        return null;
    }

    @Override
    public boolean authentication(AdminUser adminUser, UserRole funRole) {
        return adminUser.getType() >= funRole.ordinal();
    }

    @Override
    public boolean authentication(AdminUser adminUser, int funRole) {
        return adminUser.getType() != funRole;
    }

    @Override
    public String login(String userName, String password, String verifyCode) {
        AdminUser adminUser = this.lambdaQuery().eq(AdminUser::getUserName, userName)
                .eq(AdminUser::getPassword, userService.encryption(password))
                .last("limit 1").one();
        if (adminUser != null) {
//            boolean validCode = codeVerifier.isValidCode(adminUser.getGoogleCode(), verifyCode);
//            if (validCode) {
            String token = IdUtil.simpleUUID();
            stringRedisTemplate.opsForValue().set(CodeScenes.admin_login.getPev().concat(token),
                    JSONObject.toJSONString(adminUser), Duration.ofDays(7 * 2));
            return token;
            // }
        }
        return null;
    }
}
