package com.zs.pash.core.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.zs.pash.entity.ConnectionInfo;
import org.springframework.stereotype.Component;
import org.yeauty.pojo.Session;

@Component
public class CommonCache {
   private final Map<String, ConnectionInfo> mappingMap = new ConcurrentHashMap();
   private final Map<String, Integer> ipDevMap = new ConcurrentHashMap();
   private final Map<Session, AtomicInteger> pingCounter = new ConcurrentHashMap();

   public Map<String, ConnectionInfo> mappingMap() {
      return this.mappingMap;
   }

   public Map<String, Integer> ipDevMap() {
      return this.ipDevMap;
   }

   public Map<Session, AtomicInteger> pingCounter() {
      return this.pingCounter;
   }
}
