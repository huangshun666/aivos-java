package com.zs.pash.core;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zs.pash.core.cmd.CommandFactory;
import com.zs.pash.entity.WsFromText;
import com.zs.pash.service.NettyConnectService;
import io.netty.handler.codec.http.HttpHeaders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yeauty.annotation.BeforeHandshake;
import org.yeauty.annotation.OnBinary;
import org.yeauty.annotation.OnClose;
import org.yeauty.annotation.OnError;
import org.yeauty.annotation.OnMessage;
import org.yeauty.annotation.OnOpen;
import org.yeauty.annotation.ServerEndpoint;
import org.yeauty.pojo.Session;

@Component
@ServerEndpoint(
   host = "${spring-netty.host}",
   port = "${spring-netty.port}",
   path = "/ws"
)
public class WebSocketServer {
   private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
   @Autowired
   private CommandFactory commandFactory;
   @Autowired
   private NettyConnectService nettyConnectService;

   @BeforeHandshake
   public void handshake(Session session, HttpHeaders headers) {
      this.nettyConnectService.verify(session, headers);
   }

   @OnOpen
   public void onOpen(Session session, HttpHeaders headers) {
      this.nettyConnectService.connect(session, headers);
      log.info("connection success: id : {} , token:{}", session.id().asShortText(), headers.get("token"));
   }

   @OnClose
   public void onClose(Session session) throws IOException {
      this.nettyConnectService.close(session);
      log.info(" close connection  id : {} ", session.id().asShortText());
   }

   @OnError
   public void onError(Session session, Throwable throwable) {
      throwable.printStackTrace();
   }

   @OnMessage
   public void onMessage(Session session, String message) {
      boolean validObject = JSON.isValidObject(message);
      if (validObject) {
         WsFromText wsFromText = (WsFromText)JSONObject.parseObject(message, WsFromText.class);
         if (wsFromText != null) {
            this.commandFactory.execute(wsFromText, this.nettyConnectService.get(session.id().asShortText()));
         }
      }

   }

   @OnBinary
   public void onBinary(Session session, byte[] data) {
      String message = new String(data, StandardCharsets.UTF_8);
      boolean validObject = JSON.isValidObject(message);
      if (validObject) {
         WsFromText wsFromText = (WsFromText)JSONObject.parseObject(message, WsFromText.class);
         if (wsFromText != null) {
            this.commandFactory.execute(wsFromText, this.nettyConnectService.get(session.id().asShortText()));
         }
      }

   }
}
