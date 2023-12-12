package com.zs.forex.aop;

import com.zs.forex.common.vcenum.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminNeedLogin {

    /**
     * 最小身份
     * 例如 ： 如果设置 该接口的注解值为 UserRole.proxy
     * 就是大于等于 这个身份才能防伪该接口
     */
    UserRole minType() default UserRole.proxy;

    /**
     * 排除 某个 身份禁止使用
     */
    int exclude() default -1;
}
