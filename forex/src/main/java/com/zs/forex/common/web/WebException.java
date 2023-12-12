package com.zs.forex.common.web;

import lombok.Data;

@Data
public class WebException extends Exception {

    private BaseErrorInfoInterface baseErrorInfoInterface;

    public WebException(BaseErrorInfoInterface baseErrorInfoInterface) {
        super(baseErrorInfoInterface.getResultMsg());
        this.baseErrorInfoInterface = baseErrorInfoInterface;
    }
}
