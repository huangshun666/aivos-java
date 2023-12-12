package com.zs.pash.service.impl;

import java.util.Map;

import com.zs.pash.service.TopicService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@RefreshScope
@Service
public class TopicServiceImpl implements TopicService {
   private Map<String, Boolean> topics;
   @Value("${topic.limit}")
   private int limit;

   public boolean verify(int counter) {
      return counter <= this.limit;
   }

   public boolean needAuth(String topic) {
      return (Boolean)this.topics.getOrDefault(topic, false);
   }
}
