package com.zs.forex.common.web;

public enum CommonEnum implements BaseErrorInfoInterface {
    SUCCESS("200", "成功!"),
    INTERNAL_SERVER_ERROR("500", "服务器正忙，请稍后再试");

    /**
     * 错误码
     */
    private String resultCode;

    /**
     * 错误描述
     */
    private String resultMsg;

    CommonEnum(String resultCode, String resultMsg) {
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
