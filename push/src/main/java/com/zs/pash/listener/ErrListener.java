package com.zs.pash.listener;

import io.nats.client.Connection;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.support.Status;

public class ErrListener implements ErrorListener {
   public void errorOccurred(Connection conn, String error) {
      System.out.println(error);
   }

   public void exceptionOccurred(Connection conn, Exception exp) {
      exp.printStackTrace();
   }

   public void slowConsumerDetected(Connection conn, Consumer consumer) {
      ErrorListener.super.slowConsumerDetected(conn, consumer);
   }

   public void messageDiscarded(Connection conn, Message msg) {
      ErrorListener.super.messageDiscarded(conn, msg);
   }

   public void heartbeatAlarm(Connection conn, JetStreamSubscription sub, long lastStreamSequence, long lastConsumerSequence) {
      ErrorListener.super.heartbeatAlarm(conn, sub, lastStreamSequence, lastConsumerSequence);
   }

   public void unhandledStatus(Connection conn, JetStreamSubscription sub, Status status) {
      ErrorListener.super.unhandledStatus(conn, sub, status);
   }

   public void flowControlProcessed(Connection conn, JetStreamSubscription sub, String subject, FlowControlSource source) {
      ErrorListener.super.flowControlProcessed(conn, sub, subject, source);
   }
}
