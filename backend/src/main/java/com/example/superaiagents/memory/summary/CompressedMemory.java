package com.example.superaiagents.memory.summary;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 中期记忆：压缩后的对话摘要
 * 存储在 JSON 文件中
 */
public class CompressedMemory implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private List<DialogueChunk> chunks;
    private long createdAt;
    private long updatedAt;

    public CompressedMemory(String sessionId) {
        this.sessionId = sessionId;
        this.chunks = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public CompressedMemory() {
        this.chunks = new ArrayList<>();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<DialogueChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<DialogueChunk> chunks) {
        this.chunks = chunks;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addChunk(DialogueChunk chunk) {
        this.chunks.add(chunk);
        this.updatedAt = System.currentTimeMillis();
    }

    @JsonIgnore
    public String getFullSummary() {
        StringBuilder sb = new StringBuilder();
        for (DialogueChunk chunk : chunks) {
            sb.append("[段").append(chunk.getChunkIndex()).append("]")
              .append(chunk.getSummary())
              .append("; ");
        }
        return sb.toString();
    }
}
