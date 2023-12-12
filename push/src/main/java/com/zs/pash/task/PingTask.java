package com.zs.pash.task;

import java.util.concurrent.Executors;

import com.zs.pash.core.cache.CommonCache;
import com.zs.pash.service.PingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
public class PingTask implements SchedulingConfigurer {
   private static final Logger log = LoggerFactory.getLogger(PingTask.class);
   @Autowired
   private PingService pingService;
   @Autowired
   private CommonCache commonCache;

   public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2));
   }

   public void refreshInterval() {
      if (this.pingService.enableRefresh()) {
         this.pingService.pingCounter().forEach((k, v) -> {
            int andDecrement = v.decrementAndGet();
            if (andDecrement <= 0) {
               this.pingService.removeCounter(k);
            }

         });
      }

   }

   public void printfInfo() {
   }
}
