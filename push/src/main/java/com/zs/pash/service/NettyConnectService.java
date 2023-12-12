package com.zs.pash.service;

import com.zs.pash.entity.ConnectionInfo;
import io.netty.handler.codec.http.HttpHeaders;
import org.yeauty.pojo.Session;

public interface NettyConnectService {
   ConnectionInfo get(String var1);

   void connect(Session var1, HttpHeaders var2);

   void close(Session var1);

   void verify(Session var1, HttpHeaders var2);
}
