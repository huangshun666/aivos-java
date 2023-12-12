package com.zs.pash.service.impl;

import com.zs.pash.Application;
import com.zs.pash.core.cache.CommonCache;
import com.zs.pash.entity.ConnectionInfo;
import com.zs.pash.service.NatsService;
import com.zs.pash.service.NettyConnectService;
import com.zs.pash.service.PingService;
import io.nats.client.Dispatcher;
import io.netty.handler.codec.http.HttpHeaders;
import java.net.SocketAddress;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.yeauty.pojo.Session;

@RefreshScope
@Service
public class NettyConnectServiceImpl implements NettyConnectService {
   private static final Logger log = LoggerFactory.getLogger(NettyConnectServiceImpl.class);
   @Value("${spring-netty.limit}")
   private int connectLimit;
   @Autowired
   private NatsService natsService;
   @Autowired
   private PingService pingService;

   public ConnectionInfo get(String sessionKey) {
      return (ConnectionInfo)((CommonCache) Application.applicationContext.getBean(CommonCache.class)).mappingMap().get(sessionKey);
   }

   public void connect(Session session, HttpHeaders headers) {
      Dispatcher dispatcher = this.natsService.registerDispatcher(session);
      ConnectionInfo connectionInfo = new ConnectionInfo();
      connectionInfo.setUserId(-1);
      connectionInfo.setDispatcher(dispatcher);
      connectionInfo.setSession(session);
      connectionInfo.setTopics(new ArrayList());
      ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).mappingMap().put(session.id().asShortText(), connectionInfo);
      session.channel().config().setWriteBufferHighWaterMark(5242880);
      dispatcher.clearDroppedCount();
      this.pingService.addCounter(session);
   }

   public void close(Session session) {
      String sessionKey = session.id().asShortText();
      if (((CommonCache)Application.applicationContext.getBean(CommonCache.class)).mappingMap().containsKey(sessionKey)) {
         ConnectionInfo connectionInfo = (ConnectionInfo)((CommonCache)Application.applicationContext.getBean(CommonCache.class)).mappingMap().get(sessionKey);
         this.natsService.clearDispatcher(connectionInfo.getDispatcher());
         ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).mappingMap().remove(sessionKey);
      }

      SocketAddress socketAddress = session.remoteAddress();
      if (socketAddress != null) {
         String ip = socketAddress.toString().split(":")[0].substring(1);
         if (((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().containsKey(ip)) {
            Integer limit = (Integer)((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().get(ip);
            limit = limit - 1;
            if (limit <= 0) {
               ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().remove(ip);
            } else {
               ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().replace(ip, limit);
            }
         }
      }

   }

   public void verify(Session session, HttpHeaders headers) {
      if (this.connectLimit > 0) {
         SocketAddress socketAddress = session.remoteAddress();
         String ip = socketAddress.toString().split(":")[0].substring(1);
         if (((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().containsKey(ip)) {
            Integer limit = (Integer)((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().get(ip);
            limit = limit + 1;
            ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().replace(ip, limit);
            if (limit > this.connectLimit) {
               session.close();
            }
         } else {
            ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).ipDevMap().put(ip, 1);
         }

      }
   }
}
