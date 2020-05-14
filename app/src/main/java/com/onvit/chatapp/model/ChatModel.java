package com.onvit.chatapp.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChatModel {
    public Map<String, Boolean> users = new HashMap<>(); // 채팅방 유저들
    public Map<String,Comment> comments = new HashMap<>(); //채팅방의 대화내용
    public int id;

    @Override
    public String toString() {
        return "ChatModel{" +
                "users=" + users +
                ", comments=" + comments +
                ", id=" + id +
                '}';
    }

    public static class Comment implements Comparable<Comment>{
        public String uid;
        public String message;
        public long timestamp;
        public String type;
        public long unReadCount;
        public String key;

        public long getUnReadCount() {
            return unReadCount;
        }

        public void setUnReadCount(long unReadCount) {
            this.unReadCount = unReadCount;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Comment comment = (Comment) o;
            return timestamp == comment.timestamp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp);
        }

        @Override
        public String toString() {
            return "Comment{" +
                    "uid='" + uid + '\'' +
                    ", message='" + message + '\'' +
                    ", timestamp=" + timestamp +
                    ", type='" + type + '\'' +
                    ", unReadCount=" + unReadCount +
                    ", key='" + key + '\'' +
                    '}';
        }

        @Override
        public int compareTo(Comment comment) {
            if(this.timestamp - comment.getTimestamp()>0){
                return 1;
            }else if(this.timestamp - comment.getTimestamp()<0){
                return -1;
            }
            return 0;
        }
    }
}
