package com.zs.pash.core.cmd;

import com.zs.pash.entity.ConnectionInfo;
import com.zs.pash.entity.WsFromText;
import com.zs.pash.service.TopicService;
import io.nats.client.Dispatcher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubCommand implements Command {
   private static final Logger log = LoggerFactory.getLogger(SubCommand.class);
   @Autowired
   private TopicService topicService;

   public String operateType() {
      return "sub";
   }

   public void execute(WsFromText wsFromText, ConnectionInfo connectionInfo) {
      try {
         Dispatcher dispatcher = connectionInfo.getDispatcher();
         String topic = wsFromText.getTopic();
         List<String> topics = connectionInfo.getTopics();
         String subject = topic.replace("*", "0").replace(">", "0");
         int counter = topics.size();
         if (!topics.contains(topic)) {
            ++counter;
         }

         boolean verify = this.topicService.verify(counter);
         if (verify) {
            dispatcher.subscribe(subject);
            connectionInfo.setTopics(topics);
            log.info("{} ,sub success", wsFromText.getTopic());
         }

         if (!topics.contains(topic)) {
            topics.add(topic);
         }
      } catch (Exception var9) {
         var9.printStackTrace();
      }

   }
}
