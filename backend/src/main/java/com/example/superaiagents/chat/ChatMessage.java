package com.example.superaiagents.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object id;
    private String content;
    private boolean isUser;
    private Object time;

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @JsonProperty("isUser")
    public boolean isUser() {
        return isUser;
    }

    @JsonProperty("isUser")
    public void setUser(boolean user) {
        isUser = user;
    }

    public Object getTime() {
        return time;
    }

    public void setTime(Object time) {
        this.time = time;
    }
}
