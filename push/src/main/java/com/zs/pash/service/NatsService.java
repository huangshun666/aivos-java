package com.zs.pash.service;

import com.zs.pash.dto.PushMsgDTO;
import io.nats.client.Dispatcher;
import org.yeauty.pojo.Session;

public interface NatsService {
   Dispatcher registerDispatcher(Session var1);

   void clearDispatcher(Dispatcher var1);

   boolean publish(PushMsgDTO var1);
}
