package com.zs.pash.core.cmd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zs.pash.entity.ConnectionInfo;
import com.zs.pash.entity.WsFromText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CommandFactory implements CommandLineRunner {
   @Autowired
   private ApplicationContext applicationContext;
   private final Map<String, Command> commandMap = new ConcurrentHashMap();

   public void execute(WsFromText wsFromText, ConnectionInfo connectionInfo) {
      Command targetCommand = (Command)this.commandMap.get(wsFromText.getCmd());
      if (targetCommand != null) {
         targetCommand.execute(wsFromText, connectionInfo);
      }

   }

   public void run(String... args) {
      Map<String, Command> tempMap = this.applicationContext.getBeansOfType(Command.class);
      tempMap.values().forEach((source) -> {
         Command var10000 = (Command)this.commandMap.put(source.operateType(), source);
      });
   }
}
