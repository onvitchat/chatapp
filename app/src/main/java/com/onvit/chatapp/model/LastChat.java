package com.onvit.chatapp.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//마지막채팅내용 및 각자 안읽은 메세지 개수 표시.
public class LastChat implements Comparable<LastChat>, Cloneable{
    private Map<String,timeInfo> existUsers = new HashMap<>();
    private String chatName;
    private String lastChat;
    private long timestamp;

    public LastChat() {
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public Map<String,timeInfo> getExistUsers() {
        return existUsers;
    }

    public void setExistUsers(Map<String, timeInfo> existUsers) {
        this.existUsers = existUsers;
    }

    public String getLastChat() {
        return lastChat;
    }

    public void setLastChat(String lastChat) {
        this.lastChat = lastChat;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "LastChat{" +
                "existUsers=" + existUsers +
                ", chatName='" + chatName + '\'' +
                ", lastChat='" + lastChat + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public int compareTo(LastChat lastChat) {
        if(this.timestamp - lastChat.getTimestamp()>0){
            return -1;
        }else if(this.timestamp - lastChat.getTimestamp()<0){
            return 1;
        }
        return 0;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LastChat lastChat = (LastChat) o;
        return Objects.equals(chatName, lastChat.chatName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatName);
    }

    public static class timeInfo{
        private long exitTime;
        private long initTime;
        private long unReadCount;

        public long getInitTime() {
            return initTime;
        }

        public void setInitTime(long initTime) {
            this.initTime = initTime;
        }

        public long getExitTime() {
            return exitTime;
        }

        public void setExitTime(long exitTime) {
            this.exitTime = exitTime;
        }

        public long getUnReadCount() {
            return unReadCount;
        }

        public void setUnReadCount(long unReadCount) {
            this.unReadCount = unReadCount;
        }

        @Override
        public String toString() {
            return "timeInfo{" +
                    "exitTime=" + exitTime +
                    ", initTime=" + initTime +
                    ", unReadCount=" + unReadCount +
                    '}';
        }
    }
}
