package com.zs.pash.service.impl;

import com.alibaba.fastjson2.JSON;
import com.zs.pash.dto.PushMsgDTO;
import com.zs.pash.listener.ErrListener;
import com.zs.pash.listener.MsgListener;
import com.zs.pash.service.NatsService;
import com.zs.pash.tools.GZipTool;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import java.io.IOException;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yeauty.pojo.Session;

@Service
public class NatsServiceImpl implements NatsService {
   private static final Logger log = LoggerFactory.getLogger(NatsServiceImpl.class);
   @Value("${nats.url}")
   private String natsConnectionUrl;
   private Connection nc;

   @PostConstruct
   public void init() {
      try {
         Options build = (new Options.Builder()).connectionName("stock-push-" + UUID.randomUUID().toString()).maxReconnects(-1).server(this.natsConnectionUrl).errorListener(new ErrListener()).maxMessagesInOutgoingQueue(Integer.MAX_VALUE).traceConnection().build();
         this.nc = Nats.connect(build);
      } catch (InterruptedException | IOException var2) {
         throw new RuntimeException(var2);
      }
   }

   public Dispatcher registerDispatcher(Session session) {
      Dispatcher dispatcher = this.nc.createDispatcher(new MsgListener(session));
      dispatcher.setPendingLimits(536870912L, 536870912L);
      return dispatcher;
   }

   public void clearDispatcher(Dispatcher dispatcher) {
      this.nc.closeDispatcher(dispatcher);
   }

   public boolean publish(PushMsgDTO dto) {
      String subject = dto.getTopic();

      try {
         this.nc.publish(subject, GZipTool.compress(JSON.toJSONString(dto), "utf-8"));
         return true;
      } catch (Exception var4) {
         var4.printStackTrace();
         return false;
      }
   }
}
