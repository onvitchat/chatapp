package com.onvit.chatapp.model;

import java.util.List;
import java.util.Map;


public class Vote implements Comparable<Vote>{
    private String title;
    private long deadline;
    private Map<String, Object> content;
    private String type;
    private String registrant;
    private String key;
    private List<User> list;
    private String end;

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public List<User> getList() {
        return list;
    }

    public void setList(List<User> list) {
        this.list = list;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRegistrant() {
        return registrant;
    }

    public void setRegistrant(String registrant) {
        this.registrant = registrant;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "title='" + title + '\'' +
                ", deadline=" + deadline +
                ", content=" + content +
                ", type='" + type + '\'' +
                ", registrant='" + registrant + '\'' +
                ", key='" + key + '\'' +
                ", list=" + list +
                '}';
    }

    @Override
    public int compareTo(Vote vote) {
        if(this.deadline - vote.getDeadline()>0){
            return 1;
        }else if(this.deadline - vote.getDeadline()<0){
            return -1;
        }
        return 0;
    }
}

