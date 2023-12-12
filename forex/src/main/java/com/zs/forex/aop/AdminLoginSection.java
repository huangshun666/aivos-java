package com.zs.forex.aop;


import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.AdminUserService;
import com.zs.forex.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
@Slf4j
@Order(8)
@AllArgsConstructor
@Aspect
@Component
public class AdminLoginSection {

    private final AdminUserService adminUserService;

    @Pointcut("@annotation(com.zs.forex.aop.AdminNeedLogin)")
    public void verifyLogin() {
    }

    @Around("verifyLogin()")
    public Object verify(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        String token = RequestBodyWeb.get().getToken();

        AdminUser adminUser = RequestBodyWeb.get().getAdminUser();
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        AdminNeedLogin adminNeedLogin = signature.getMethod().getAnnotation(AdminNeedLogin.class);

        //验证登录
        boolean adminByToken = adminUserService.getAdminByToken(token);
        boolean b = StringUtils.hasLength(token);
        if (!b || !adminByToken){
            log.info(" not login {} .{} ,{}", adminByToken,b,token);
            return ResultBody.error(RespCodeEnum.not_login);

        }

        //鉴权
        if (!adminUserService.authentication(adminUser, adminNeedLogin.minType()) ||
                !adminUserService.authentication(adminUser, adminNeedLogin.exclude())) {
            return ResultBody.error(RespCodeEnum.not_permissions);
        }


        return proceedingJoinPoint.proceed();

    }
}
