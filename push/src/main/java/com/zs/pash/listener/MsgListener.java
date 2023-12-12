package com.zs.pash.listener;

import com.alibaba.fastjson2.JSON;
import com.zs.pash.dto.PushMsgDTO;
import com.zs.pash.tools.GZipTool;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yeauty.pojo.Session;

public class MsgListener implements MessageHandler {
   private static final Logger log = LoggerFactory.getLogger(MsgListener.class);
   private Session session;

   public void onMessage(Message msg) {
      try {
         if (this.session.isWritable()) {
            byte[] decompress = GZipTool.decompress(msg.getData());
            String text = new String(decompress, StandardCharsets.UTF_8);
            String subject = msg.getSubject();
            PushMsgDTO dto = new PushMsgDTO();
            dto.setTopic(subject);
            dto.setData(text);
            this.session.sendBinary(GZipTool.compress(JSON.toJSONString(dto), "utf-8"));
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

   }

   public MsgListener(Session session) {
      this.session = session;
   }
}
