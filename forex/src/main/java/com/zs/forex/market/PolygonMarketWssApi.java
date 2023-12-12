package com.zs.forex.market;

import com.zs.forex.handler.MarketHandler;
import lombok.Data;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Data
public class PolygonMarketWssApi {


    private final String url;

    private final MarketHandler marketHandler;


    public PolygonMarketWssApi(String url, MarketHandler marketHandler) {
        this.url = url;
        this.marketHandler = marketHandler;
    }

    private WebSocket webSocket;

    public void connection() {
        if (webSocket != null) {
            webSocket = null;
        }
        OkHttpClient mClient = new OkHttpClient.Builder()
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        webSocket = mClient.newWebSocket(request, marketHandler);

    }


    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "close");
            webSocket = null;
        }
    }

    public void send(String msg) {
        if (webSocket != null)
            webSocket.send(msg);
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }
}
