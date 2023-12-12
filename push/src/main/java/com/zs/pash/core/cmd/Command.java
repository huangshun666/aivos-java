package com.zs.pash.core.cmd;


import com.zs.pash.entity.ConnectionInfo;
import com.zs.pash.entity.WsFromText;

public interface Command {
   String operateType();

   void execute(WsFromText var1, ConnectionInfo var2);
}
