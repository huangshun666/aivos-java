package com.zs.pash.core.cmd;

import com.zs.pash.entity.ConnectionInfo;
import com.zs.pash.entity.WsFromText;
import com.zs.pash.service.TopicService;
import io.nats.client.Dispatcher;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnSubCommand implements Command {
   @Autowired
   private TopicService topicService;

   public String operateType() {
      return "unsub";
   }

   public void execute(WsFromText wsFromText, ConnectionInfo connectionInfo) {
      Dispatcher dispatcher = connectionInfo.getDispatcher();
      List<String> topics = connectionInfo.getTopics();
      String topic = wsFromText.getTopic();
      topics.remove(topic);
      dispatcher.unsubscribe(topic);
      connectionInfo.setTopics(topics);
   }
}
