package com.zs.pash.dto;

public class PushMsgDTO {
   private String topic;
   private Object data;

   public String getTopic() {
      return this.topic;
   }

   public Object getData() {
      return this.data;
   }

   public void setTopic(String topic) {
      this.topic = topic;
   }

   public void setData(Object data) {
      this.data = data;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PushMsgDTO)) {
         return false;
      } else {
         PushMsgDTO other = (PushMsgDTO)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
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
      return other instanceof PushMsgDTO;
   }

   public int hashCode() {
      boolean PRIME = true;
      int result = 1;
      Object $topic = this.getTopic();
      result = result * 59 + ($topic == null ? 43 : $topic.hashCode());
      Object $data = this.getData();
      result = result * 59 + ($data == null ? 43 : $data.hashCode());
      return result;
   }

   public String toString() {
      return "PushMsgDTO(topic=" + this.getTopic() + ", data=" + this.getData() + ")";
   }
}
