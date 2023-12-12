package com.zs.forex.common.vcenum;

public enum SymbolType {
    Forex("wss://socket.polygon.io/forex", "{\"action\":\"subscribe\", \"params\":\"C.{pair}\"}"),          //外汇

    Metal(null, null),           //贵金属

    Crude(null, null),            //原油

    Index("wss://socket.polygon.io/indices", "{\"action\":\"subscribe\", \"params\":\"V.I:{pair}\"}"),            //指数

    Crypto("wss://socket.polygon.io/crypto", "{\"action\":\"subscribe\", \"params\":\"XT.{pair}\"}"), //加密货币
    ;

    private final String url;

    private final String cmd;

    SymbolType(String url, String cmd) {
        this.url = url;
        this.cmd = cmd;
    }

    public String getUrl() {
        return url;
    }

    public String getCmd() {
        return cmd;
    }
}
