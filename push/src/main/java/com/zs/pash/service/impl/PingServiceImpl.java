package com.zs.pash.service.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.zs.pash.Application;
import com.zs.pash.core.cache.CommonCache;
import com.zs.pash.service.PingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.yeauty.pojo.Session;

@RefreshScope
@Service
public class PingServiceImpl implements PingService {
   private static final Logger log = LoggerFactory.getLogger(PingServiceImpl.class);
   @Value("${spring-netty.ping.interval}")
   private Integer pingInterval;
   @Value("${spring-netty.ping.refresh}")
   private boolean enableRefresh;

   public void refreshCounter(Session session) {
      if (((CommonCache) Application.applicationContext.getBean(CommonCache.class)).pingCounter().containsKey(session)) {
         ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).pingCounter().replace(session, new AtomicInteger(this.pingInterval));
      }

   }

   public Map<Session, AtomicInteger> pingCounter() {
      return ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).pingCounter();
   }

   public void addCounter(Session session) {
      ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).pingCounter().put(session, new AtomicInteger(this.pingInterval));
   }

   public void removeCounter(Session session) {
      ((CommonCache)Application.applicationContext.getBean(CommonCache.class)).pingCounter().remove(session);
      session.close();
   }

   public boolean enableRefresh() {
      return this.enableRefresh;
   }
}
