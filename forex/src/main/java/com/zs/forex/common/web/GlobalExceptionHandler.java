package com.zs.forex.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResultBody exceptionHandler(HttpServletRequest req, Exception e) {
        e.printStackTrace();
        return ResultBody.error(CommonEnum.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(value = WebException.class)
    @ResponseBody
    public ResultBody exceptionHandler(HttpServletRequest req, WebException e) {
        ResultBody error = ResultBody.error(e.getBaseErrorInfoInterface());

        log.error("业务异常:code :{} msg:{}", error.getCode(), error.getMessage());

        log.info("Time-Consuming : {} ms", 0);

        // 接口结束后换行，方便分割查看
        log.info("=========================================== End ===========================================" + LINE_SEPARATOR);
      
        return error;
    }

}
