package com.example.superaiagents.memory.session;

import com.example.superaiagents.chatmemory.FileBasedChatMemory;
import com.example.superaiagents.memory.summary.CompressedMemory;
import com.example.superaiagents.memory.summary.DialogueChunk;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 短期记忆服务 - Redis 存储
 */
@Service
public class SessionMemoryService {

    private static final Logger log = LoggerFactory.getLogger(SessionMemoryService.class);

    private static final String SESSION_KEY_PREFIX = "memory:session:";
    private static final long SESSION_EXPIRE_SECONDS = 24 * 60 * 60; // 24小时

    @Resource
    private JedisPooled jedisPooled;

    private final FileBasedChatMemory fileBasedChatMemory;

    private final Kryo kryo;

    public SessionMemoryService() {
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        this.fileBasedChatMemory = new FileBasedChatMemory(fileDir);
        this.kryo = new Kryo();
        kryo.setRegistrationRequired(false);
    }

    /**
     * 获取或创建会话记忆
     */
    public SessionMemory getOrCreateSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        byte[] redisKey = redisKey(key);
        byte[] serialized = jedisPooled.get(redisKey);
        if (serialized == null) {
            return new SessionMemory(sessionId);
        }
        try (Input input = new Input(new ByteArrayInputStream(serialized))) {
            SessionMemory session;
            synchronized (kryo) {
                session = kryo.readObject(input, SessionMemory.class);
            }
            if (!session.hasValidMessages()) {
                log.warn("Session {} contains invalid message data, clearing session memory", sessionId);
                jedisPooled.del(redisKey);
                return new SessionMemory(sessionId);
            }
            return session;
        } catch (Exception e) {
            log.warn("Failed to deserialize session memory, creating new one: {}", e.getMessage());
            jedisPooled.del(redisKey);
            return new SessionMemory(sessionId);
        }
    }

    /**
     * 添加消息到会话
     */
    public void addMessages(String sessionId, List<Message> messages) {
        SessionMemory session = getOrCreateSession(sessionId);
        session.addMessages(messages);
        saveSession(sessionId, session);
        log.info("Session {} 添加 {} 条消息，当前轮数: {}", sessionId, messages.size(), session.getMessageCount());
    }

    /**
     * 获取会话消息
     */
    public List<Message> getMessages(String sessionId) {
        SessionMemory session = getOrCreateSession(sessionId);
        return session.getMessages();
    }

    /**
     * 获取当前轮数
     */
    public int getMessageCount(String sessionId) {
        SessionMemory session = getOrCreateSession(sessionId);
        return session.getMessageCount();
    }

    /**
     * 清空会话消息
     */
    public void clearSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        jedisPooled.del(redisKey(key));
        log.info("Session {} 已清空", sessionId);
    }

    /**
     * 清空但保留最后几条消息
     */
    public void clearSessionKeepLast(String sessionId, int keepCount) {
        SessionMemory session = getOrCreateSession(sessionId);
        session.clearMessagesKeepLast(keepCount);
        saveSession(sessionId, session);
        log.info("Session {} 已清空，保留最后 {} 条消息", sessionId, keepCount);
    }

    private void saveSession(String sessionId, SessionMemory session) {
        String key = SESSION_KEY_PREFIX + sessionId;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            synchronized (kryo) {
                kryo.writeObject(output, session);
            }
            output.close();
            byte[] bytes = baos.toByteArray();
            jedisPooled.setex(redisKey(key), SESSION_EXPIRE_SECONDS, bytes);
        } catch (Exception e) {
            log.error("Failed to save session memory: {}", e.getMessage(), e);
        }
    }

    private byte[] redisKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 保存压缩后的摘要到 .kryo 文件
     */
    public void saveCompressedMemory(String sessionId, DialogueChunk chunk) {
        CompressedMemory compressedMemory = loadOrCreateCompressedMemory(sessionId);
        compressedMemory.addChunk(chunk);
        log.info("Session {} 保存压缩摘要块 #{}", sessionId, chunk.getChunkIndex());
    }

    /**
     * 加载或创建压缩记忆
     */
    private CompressedMemory loadOrCreateCompressedMemory(String sessionId) {
        String summaryFilePath = System.getProperty("user.dir") + "/tmp/chat-memory/" + sessionId + "_summary.kryo";
        File file = new File(summaryFilePath);
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                CompressedMemory memory;
                synchronized (kryo) {
                    memory = kryo.readObject(input, CompressedMemory.class);
                }
                log.info("Loaded existing compressed memory for session {}, {} chunks",
                        sessionId, memory.getChunks().size());
                return memory;
            } catch (Exception e) {
                log.warn("Failed to load compressed memory, creating new one: {}", e.getMessage());
            }
        }
        return new CompressedMemory(sessionId);
    }

    /**
     * 保存压缩记忆到文件
     */
    public void persistCompressedMemory(CompressedMemory compressedMemory) {
        String summaryFilePath = System.getProperty("user.dir") + "/tmp/chat-memory/"
                + compressedMemory.getSessionId() + "_summary.kryo";
        try (Output output = new Output(new FileOutputStream(summaryFilePath))) {
            synchronized (kryo) {
                kryo.writeObject(output, compressedMemory);
            }
            log.info("Persisted compressed memory for session {}", compressedMemory.getSessionId());
        } catch (Exception e) {
            log.error("Failed to persist compressed memory: {}", e.getMessage(), e);
        }
    }

    /**
     * 加载压缩记忆
     */
    public CompressedMemory loadCompressedMemory(String sessionId) {
        return loadOrCreateCompressedMemory(sessionId);
    }
}
