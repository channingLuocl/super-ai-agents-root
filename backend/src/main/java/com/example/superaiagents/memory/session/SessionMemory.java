package com.example.superaiagents.memory.session;

import org.springframework.ai.chat.messages.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 短期记忆：存储当前会话的原始消息
 * 存储在 Redis 中
 */
public class SessionMemory implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private List<Message> messages;
    private int messageCount;
    private long lastActiveTime;

    public SessionMemory(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new ArrayList<>();
        this.messageCount = 0;
        this.lastActiveTime = System.currentTimeMillis();
    }

    public SessionMemory() {
        this.messages = new ArrayList<>();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean hasValidMessages() {
        Object currentMessages = messages;
        if (!(currentMessages instanceof List<?> messageList)) {
            return false;
        }
        return messageList.stream().allMatch(Message.class::isInstance);
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public void addMessages(List<Message> newMessages) {
        this.messages.addAll(newMessages);
        this.messageCount += newMessages.size();
        this.lastActiveTime = System.currentTimeMillis();
    }

    public void clearMessages() {
        this.messages.clear();
        this.lastActiveTime = System.currentTimeMillis();
    }

    public void clearMessagesKeepLast(int keepCount) {
        if (this.messages.size() > keepCount) {
            List<Message> kept = new ArrayList<>(this.messages.subList(
                    this.messages.size() - keepCount, this.messages.size()));
            this.messages.clear();
            this.messages.addAll(kept);
        }
        this.lastActiveTime = System.currentTimeMillis();
    }
}
