package com.zs.self.tenum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradeDateEnum {

    before(0, "盘前交易"),

    m_trade(1, "早盘交易"),

    a_trade(2, "午盘交易"),

    rest(3, "午间休盘"),

    after(4, "盘后交易"),

    cq(5, "已收盘"),

    cd(6, "休市");

    final int code;

    final String desc;
}
