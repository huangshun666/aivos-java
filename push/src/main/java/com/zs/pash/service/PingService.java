package com.zs.pash.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.yeauty.pojo.Session;

public interface PingService {
   void refreshCounter(Session var1);

   Map<Session, AtomicInteger> pingCounter();

   void addCounter(Session var1);

   void removeCounter(Session var1);

   boolean enableRefresh();
}
