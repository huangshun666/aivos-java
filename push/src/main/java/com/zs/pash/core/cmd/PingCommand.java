package com.zs.pash.core.cmd;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.zs.pash.entity.ConnectionInfo;
import com.zs.pash.entity.WsFromText;
import com.zs.pash.service.PingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PingCommand implements Command {
   @Autowired
   private PingService pingService;

   public String operateType() {
      return "ping";
   }

   public void execute(WsFromText wsFromText, ConnectionInfo connectionInfo) {
      this.pingService.refreshCounter(connectionInfo.getSession());
      JSONObject data = new JSONObject();
      data.put("pong", System.currentTimeMillis());
      connectionInfo.getSession().sendBinary(data.toJSONBBytes(new JSONWriter.Feature[0]));
   }
}
