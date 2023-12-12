package com.zs.forex.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zs.forex.aop.AdminNeedLogin;
import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.vcenum.CodeScenes;
import com.zs.forex.common.vcenum.UserRole;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.AdminUserService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

@AllArgsConstructor
@RestController
public class AdminUserController {


    private final SecretGenerator secretGenerator;
    private final AdminUserService adminUserService;

    @PostMapping("admin/user/getAdminByToken")
    @AdminNeedLogin
    public ResultBody getAdminByToken() {
        String token = RequestBodyWeb.get().getToken();
        return ResultBody.success(adminUserService.getAdminUserByToken(token));
    }

    @PostMapping("admin/user/loginAdmin")
    public ResultBody loginAdmin(@RequestBody JSONObject jsonObject) {
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        String verifyCode = jsonObject.getString("verifyCode");
        String login = adminUserService.login(username, password, verifyCode);
        if (login != null) {
            return ResultBody.success(login);
        }
        return ResultBody.error(RespCodeEnum.auth_error);
    }

    @PostMapping("admin/user/adminList")
    @AdminNeedLogin
    public ResultBody adminList(@RequestBody JSONObject jsonObject) {
        Integer pageIndex = (Integer) jsonObject.getOrDefault("pageIndex", 1);
        Integer pageSize = (Integer) jsonObject.getOrDefault("pageSize", 20);
        AdminUser adminUser = RequestBodyWeb.get().getAdminUser();
        Page<AdminUser> page = adminUserService.lambdaQuery().eq(adminUser.getType()
                        != UserRole.administrator.ordinal(), AdminUser::getCreateId, adminUser.getId())
                .page(new Page<>(pageIndex, pageSize));
        return ResultBody.success(page);
    }

    @PostMapping("admin/user/set/AdminUser")
    @AdminNeedLogin(minType = UserRole.leader, exclude = 3)
    public ResultBody setAdminUser(@RequestBody JSONObject jsonObject) throws CodeGenerationException {
        Integer id = jsonObject.getInteger("id");
        Integer type = jsonObject.getInteger("type");
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        if (!ObjectUtil.isAllNotEmpty(id, type, username, password))
            return ResultBody.error(RespCodeEnum.parameter_exception);
        AdminUser adminLUser = RequestBodyWeb.get().getAdminUser();
        if (type >= adminLUser.getType()) {
            return ResultBody.error(RespCodeEnum.not_permissions);
        }
        AdminUser adminUser = new AdminUser();
        adminUser.setCtime(new Date());
        adminUser.setId(id);
        adminUser.setUserName(username);
        adminUser.setPassword(password);
        adminUser.setType(type);
        adminUser.setGoogleCode(secretGenerator.generate());
        adminUser.setCreateId(adminLUser.getId());
        return ResultBody.success(adminUserService.saveOrUpdate(adminUser));
    }


}
