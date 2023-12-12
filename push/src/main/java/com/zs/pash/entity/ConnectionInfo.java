package com.zs.pash.entity;

import io.nats.client.Dispatcher;
import java.util.List;
import org.yeauty.pojo.Session;

public class ConnectionInfo {
   private Dispatcher dispatcher;
   private Integer userId;
   private List<String> topics;
   private Session session;

   public Dispatcher getDispatcher() {
      return this.dispatcher;
   }

   public Integer getUserId() {
      return this.userId;
   }

   public List<String> getTopics() {
      return this.topics;
   }

   public Session getSession() {
      return this.session;
   }

   public void setDispatcher(Dispatcher dispatcher) {
      this.dispatcher = dispatcher;
   }

   public void setUserId(Integer userId) {
      this.userId = userId;
   }

   public void setTopics(List<String> topics) {
      this.topics = topics;
   }

   public void setSession(Session session) {
      this.session = session;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ConnectionInfo)) {
         return false;
      } else {
         ConnectionInfo other = (ConnectionInfo)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label59: {
               Object this$dispatcher = this.getDispatcher();
               Object other$dispatcher = other.getDispatcher();
               if (this$dispatcher == null) {
                  if (other$dispatcher == null) {
                     break label59;
                  }
               } else if (this$dispatcher.equals(other$dispatcher)) {
                  break label59;
               }

               return false;
            }

            Object this$userId = this.getUserId();
            Object other$userId = other.getUserId();
            if (this$userId == null) {
               if (other$userId != null) {
                  return false;
               }
            } else if (!this$userId.equals(other$userId)) {
               return false;
            }

            Object this$topics = this.getTopics();
            Object other$topics = other.getTopics();
            if (this$topics == null) {
               if (other$topics != null) {
                  return false;
               }
            } else if (!this$topics.equals(other$topics)) {
               return false;
            }

            Object this$session = this.getSession();
            Object other$session = other.getSession();
            if (this$session == null) {
               if (other$session != null) {
                  return false;
               }
            } else if (!this$session.equals(other$session)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ConnectionInfo;
   }

   public int hashCode() {
      boolean PRIME = true;
      int result = 1;
      Object $dispatcher = this.getDispatcher();
      result = result * 59 + ($dispatcher == null ? 43 : $dispatcher.hashCode());
      Object $userId = this.getUserId();
      result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
      Object $topics = this.getTopics();
      result = result * 59 + ($topics == null ? 43 : $topics.hashCode());
      Object $session = this.getSession();
      result = result * 59 + ($session == null ? 43 : $session.hashCode());
      return result;
   }

   public String toString() {
      return "ConnectionInfo(dispatcher=" + this.getDispatcher() + ", userId=" + this.getUserId() + ", topics=" + this.getTopics() + ", session=" + this.getSession() + ")";
   }
}
