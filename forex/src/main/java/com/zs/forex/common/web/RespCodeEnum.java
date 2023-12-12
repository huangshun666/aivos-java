package com.zs.forex.common.web;

public enum RespCodeEnum implements BaseErrorInfoInterface {

    not_login("4003", "未登录"),

    not_permissions("4005", "权限不足"),

    parameter_exception("5300", "参数异常"),


    verification_code_error("5301", "验证码错误"),


    mail_error("5302", "邮箱格式不正确"),


    phone_error("5303", "手机号格式不正确"),

    auth_error("5304", "账号或密码错误"),

    mail_exist("5305", "邮箱已注册"),

    phone_exist("5306", "手机号已注册"),

    mail_not_exist("5307", "邮箱未注册"),

    phone_not_exist("5308", "手机号未注册"),

    verification_used_or_expired("5309", "验证码已使用或已过期"),
    insufficient_balance("5401", "余额不足"),

    not_certified("5402", "未认证"),

    prohibition_trading("5403", "禁止交易"),

    position_locked("5404", "持仓锁定"),

    account_locked("5405", "账号锁定"),

    not_time("5406", "未到交易时间"),

    trade_pwd("5407", "交易密码错误"),

    repeat_binding("5408", "重复绑定"),

    old_password_wrong("5409", "密码错误"),

    repeat_submit("5410", "重复提交"),





    ;

    /**
     * 错误码
     */
    private final String resultCode;

    /**
     * 错误描述
     */
    private final String resultMsg;

    RespCodeEnum(String resultCode, String resultMsg) {
        this.resultCode = resultCode;
        this.resultMsg = resultMsg;
    }

    @Override
    public String getResultCode() {
        return resultCode;
    }

    @Override
    public String getResultMsg() {
        return resultMsg;
    }
}
