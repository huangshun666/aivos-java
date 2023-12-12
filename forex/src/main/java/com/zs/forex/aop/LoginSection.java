package com.zs.forex.aop;


import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.common.web.RespCodeEnum;
import com.zs.forex.common.web.ResultBody;
import com.zs.forex.service.UserService;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Order(9)
@AllArgsConstructor
@Aspect
@Component
public class LoginSection {

    private final UserService userService;

    @Pointcut("@annotation(com.zs.forex.aop.NeedLogin)")
    public void verifyLogin() {
    }

    @Around("verifyLogin()")
    public Object verify(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String token = RequestBodyWeb.get().getToken();
        if (!StringUtils.hasLength(token) || userService.getByToken(token) == null)
            return ResultBody.error(RespCodeEnum.not_login);
        return proceedingJoinPoint.proceed();

    }
}
