package com.zs.forex.common.web;

import lombok.Data;

@Data
public class ResultBody {

    private String code;


    private String message;

    private Object result;


    public static ResultBody success() {
        return success(null);
    }


    public static ResultBody success(Object data) {
        ResultBody rb = new ResultBody();
        rb.setCode(CommonEnum.SUCCESS.getResultCode());
        rb.setMessage(CommonEnum.SUCCESS.getResultMsg());
        rb.setResult(data);
        return rb;
    }

    public static ResultBody error(BaseErrorInfoInterface errorInfo) {
        ResultBody rb = new ResultBody();
        rb.setCode(errorInfo.getResultCode());
        rb.setMessage(I18nMessageUtil.getMessage(errorInfo));
        rb.setResult(null);
        return rb;
    }

    public static ResultBody error(String code, String message) {
        ResultBody rb = new ResultBody();
        rb.setCode(code);
        rb.setMessage(message);
        rb.setResult(null);
        return rb;
    }


    public static ResultBody error(String message) {
        ResultBody rb = new ResultBody();
        rb.setCode(CommonEnum.INTERNAL_SERVER_ERROR.getResultCode());
        rb.setMessage(message);
        rb.setResult(null);
        return rb;
    }
}
