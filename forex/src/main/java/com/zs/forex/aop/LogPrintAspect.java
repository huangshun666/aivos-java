package com.zs.forex.aop;

import com.alibaba.fastjson.JSONObject;
import com.zs.forex.common.pojo.AdminUser;
import com.zs.forex.common.pojo.User;
import com.zs.forex.common.tools.IpTool;
import com.zs.forex.common.web.RequestBodyWeb;
import com.zs.forex.service.AdminUserService;
import com.zs.forex.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

@Order(0)
@AllArgsConstructor
@Aspect
@Component
@Slf4j
public class LogPrintAspect {


    private static final String LINE_SEPARATOR = System.lineSeparator();


    private final UserService userService;

    private final AdminUserService adminUserService;


    @Pointcut("execution(public * com.zs.forex.controller.*.*(..))")
    public void logPrint() {
    }


    @Before("logPrint()")
    public void doBefore(JoinPoint joinPoint) {
        // 开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        HttpServletRequest request = attributes.getRequest();

        // 打印请求相关参数
        log.info("========================================== Start ==========================================");
        // 打印请求 url
        log.info("URL            : {}", request.getRequestURL().toString());
        // 打印 Http method
        log.info("HTTP Method    : {}", request.getMethod());
        // 打印调用 controller 的全路径以及执行方法
        log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        // 打印请求的 IP
        String ipAddr = IpTool.getIpAddr(request);

        String lang = request.getHeader("lang");

        String token = request.getHeader("token");

        String device = request.getHeader("device");


        String adminToken = request.getHeader("auth");

        String deviceNo = request.getHeader("sn");

        log.info("IP             : {}", ipAddr);
        log.info("lang           : {}", lang);
        log.info("token          : {}", token);
        log.info("device         : {}", device);
        log.info("adminToken     : {}", adminToken);
        log.info("deviceNo     : {}", deviceNo);

        Optional<Object> first = Arrays.stream(joinPoint.getArgs()).filter(item -> item instanceof MultipartFile).findFirst();
        if (!first.isPresent()) {
            // 打印请求入参
            log.info("Request Args   : {}", JSONObject.toJSON(joinPoint.getArgs()));
        }
        this.afterHandler(token, adminToken, ipAddr, device, lang, adminToken, deviceNo);

    }


    @Around("logPrint()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object result = proceedingJoinPoint.proceed();

        log.info("Time-Consuming : {} ms", System.currentTimeMillis() - startTime);

        // 接口结束后换行，方便分割查看
        log.info("=========================================== End ===========================================" + LINE_SEPARATOR);

        return result;
    }

    private void afterHandler(String token, String adminToken, String ipAddr, String device,
                              String lang
            , String auth, String sn) {

        User user = null;
        AdminUser adminUser = null;
        if (token != null) {
            user = userService.getByToken(token);
        }

        if (adminToken != null) {
            adminUser = adminUserService.getAdminUserByToken(adminToken);
        }
        RequestBodyWeb.RequestBodyDTO dto = RequestBodyWeb.RequestBodyDTO.builder()
                .ip(ipAddr).device(device).token(token == null ? auth : token).lang(lang)
                .user(user)
                .adminUser(adminUser)
                .deviceNo(sn)
                .build();
        RequestBodyWeb.set(dto);
    }

}
