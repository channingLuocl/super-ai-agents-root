package com.example.superaiagents.memory.summary;

import java.io.Serializable;

/**
 * 对话摘要块：每10轮对话的压缩摘要
 */
public class DialogueChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    private int chunkIndex;
    private String summary;
    private String keyTopics;
    private int originalMessageCount;
    private long timeRangeStart;
    private long timeRangeEnd;

    public DialogueChunk() {
    }

    public DialogueChunk(int chunkIndex, String summary, String keyTopics,
                        int originalMessageCount, long timeRangeStart, long timeRangeEnd) {
        this.chunkIndex = chunkIndex;
        this.summary = summary;
        this.keyTopics = keyTopics;
        this.originalMessageCount = originalMessageCount;
        this.timeRangeStart = timeRangeStart;
        this.timeRangeEnd = timeRangeEnd;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeyTopics() {
        return keyTopics;
    }

    public void setKeyTopics(String keyTopics) {
        this.keyTopics = keyTopics;
    }

    public int getOriginalMessageCount() {
        return originalMessageCount;
    }

    public void setOriginalMessageCount(int originalMessageCount) {
        this.originalMessageCount = originalMessageCount;
    }

    public long getTimeRangeStart() {
        return timeRangeStart;
    }

    public void setTimeRangeStart(long timeRangeStart) {
        this.timeRangeStart = timeRangeStart;
    }

    public long getTimeRangeEnd() {
        return timeRangeEnd;
    }

    public void setTimeRangeEnd(long timeRangeEnd) {
        this.timeRangeEnd = timeRangeEnd;
    }
}
