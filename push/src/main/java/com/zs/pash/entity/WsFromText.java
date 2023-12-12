package com.zs.pash.entity;

public class WsFromText {
   private String cmd;
   private String topic;
   private String data;

   public String getCmd() {
      return this.cmd;
   }

   public String getTopic() {
      return this.topic;
   }

   public String getData() {
      return this.data;
   }

   public void setCmd(String cmd) {
      this.cmd = cmd;
   }

   public void setTopic(String topic) {
      this.topic = topic;
   }

   public void setData(String data) {
      this.data = data;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof WsFromText)) {
         return false;
      } else {
         WsFromText other = (WsFromText)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label47: {
               Object this$cmd = this.getCmd();
               Object other$cmd = other.getCmd();
               if (this$cmd == null) {
                  if (other$cmd == null) {
                     break label47;
                  }
               } else if (this$cmd.equals(other$cmd)) {
                  break label47;
               }

               return false;
            }

            Object this$topic = this.getTopic();
            Object other$topic = other.getTopic();
            if (this$topic == null) {
               if (other$topic != null) {
                  return false;
               }
            } else if (!this$topic.equals(other$topic)) {
               return false;
            }

            Object this$data = this.getData();
            Object other$data = other.getData();
            if (this$data == null) {
               if (other$data != null) {
                  return false;
               }
            } else if (!this$data.equals(other$data)) {
               return false;
            }

            return true;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof WsFromText;
   }

   public int hashCode() {
      boolean PRIME = true;
      int result = 1;
      Object $cmd = this.getCmd();
      result = result * 59 + ($cmd == null ? 43 : $cmd.hashCode());
      Object $topic = this.getTopic();
      result = result * 59 + ($topic == null ? 43 : $topic.hashCode());
      Object $data = this.getData();
      result = result * 59 + ($data == null ? 43 : $data.hashCode());
      return result;
   }

   public String toString() {
      return "WsFromText(cmd=" + this.getCmd() + ", topic=" + this.getTopic() + ", data=" + this.getData() + ")";
   }
}
