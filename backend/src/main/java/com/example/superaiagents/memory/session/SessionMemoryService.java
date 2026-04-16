package com.example.superaiagents.memory.session;

import com.example.superaiagents.memory.summary.CompressedMemory;
import com.example.superaiagents.memory.summary.DialogueChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短期记忆服务 - JSON/JSONL 文件存储
 */
@Service
public class SessionMemoryService {

    private static final Logger log = LoggerFactory.getLogger(SessionMemoryService.class);

    private final Path memoryDir;
    private final Path sessionsDir;
    private final Path summariesDir;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public SessionMemoryService() {
        this.memoryDir = Path.of(System.getProperty("user.dir"), "memory");
        this.sessionsDir = memoryDir.resolve("sessions");
        this.summariesDir = memoryDir.resolve("summaries");
        this.objectMapper = new ObjectMapper();
        createDirectories();
    }

    /**
     * 获取或创建会话记忆
     */
    public SessionMemory getOrCreateSession(String sessionId) {
        Object lock = lockFor(sessionId);
        synchronized (lock) {
            Path file = sessionFile(sessionId);
            SessionMemory session = new SessionMemory(sessionId);
            if (!Files.exists(file)) {
                return session;
            }
            try {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                List<Message> messages = new ArrayList<>();
                for (String line : lines) {
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    StoredMessage storedMessage = objectMapper.readValue(line, StoredMessage.class);
                    messages.add(toMessage(storedMessage));
                }
                session.setMessages(messages);
                session.setMessageCount(messages.size());
                session.setLastActiveTime(Files.getLastModifiedTime(file).toMillis());
                return session;
            } catch (Exception e) {
                log.warn("Failed to load session memory from JSONL, creating new one: {}", e.getMessage());
                return session;
            }
        }
    }

    /**
     * 添加消息到会话
     */
    public void addMessages(String sessionId, List<Message> messages) {
        Object lock = lockFor(sessionId);
        synchronized (lock) {
            appendMessages(sessionId, messages);
            int messageCount = countStoredMessages(sessionId);
            log.info("Session {} 追加 {} 条消息，当前消息数: {}", sessionId, messages.size(), messageCount);
        }
    }

    /**
     * 获取会话消息
     */
    public List<Message> getMessages(String sessionId) {
        return getOrCreateSession(sessionId).getMessages();
    }

    /**
     * 获取当前消息数
     */
    public int getMessageCount(String sessionId) {
        return getOrCreateSession(sessionId).getMessageCount();
    }

    /**
     * 清空会话消息
     */
    public void clearSession(String sessionId) {
        Object lock = lockFor(sessionId);
        synchronized (lock) {
            try {
                Files.deleteIfExists(sessionFile(sessionId));
                log.info("Session {} 已清空", sessionId);
            } catch (IOException e) {
                log.error("Failed to clear session memory: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 删除当前会话关联的短期和中期记忆文件
     */
    public void deleteSessionFiles(String sessionId) {
        Object sessionLock = lockFor(sessionId);
        synchronized (sessionLock) {
            try {
                Files.deleteIfExists(sessionFile(sessionId));
            } catch (IOException e) {
                log.error("Failed to delete session memory file: {}", e.getMessage(), e);
            }
        }
        Object summaryLock = lockFor("summary:" + sessionId);
        synchronized (summaryLock) {
            try {
                Files.deleteIfExists(summaryFile(sessionId));
            } catch (IOException e) {
                log.error("Failed to delete compressed memory file: {}", e.getMessage(), e);
            }
        }
        log.info("Session {} 关联记忆文件已删除", sessionId);
    }

    /**
     * 清空但保留最后几条消息
     */
    public void clearSessionKeepLast(String sessionId, int keepCount) {
        Object lock = lockFor(sessionId);
        synchronized (lock) {
            List<String> keptLines = readStoredMessageLines(sessionId);
            if (keptLines.size() > keepCount) {
                keptLines = new ArrayList<>(keptLines.subList(keptLines.size() - keepCount, keptLines.size()));
            }
            rewriteSessionLines(sessionId, keptLines);
            log.info("Session {} 已清空，保留最后 {} 条消息", sessionId, keepCount);
        }
    }

    /**
     * 保存压缩后的摘要到 JSON 文件
     */
    public void saveCompressedMemory(String sessionId, DialogueChunk chunk) {
        CompressedMemory compressedMemory = loadOrCreateCompressedMemory(sessionId);
        compressedMemory.addChunk(chunk);
        persistCompressedMemory(compressedMemory);
        log.info("Session {} 保存压缩摘要块 #{}", sessionId, chunk.getChunkIndex());
    }

    /**
     * 保存压缩记忆到文件
     */
    public void persistCompressedMemory(CompressedMemory compressedMemory) {
        Object lock = lockFor("summary:" + compressedMemory.getSessionId());
        synchronized (lock) {
            try {
                writeJsonAtomically(summaryFile(compressedMemory.getSessionId()), compressedMemory);
                log.info("Persisted compressed memory for session {}", compressedMemory.getSessionId());
            } catch (Exception e) {
                log.error("Failed to persist compressed memory: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 加载压缩记忆
     */
    public CompressedMemory loadCompressedMemory(String sessionId) {
        return loadOrCreateCompressedMemory(sessionId);
    }

    private CompressedMemory loadOrCreateCompressedMemory(String sessionId) {
        Object lock = lockFor("summary:" + sessionId);
        synchronized (lock) {
            Path file = summaryFile(sessionId);
            if (!Files.exists(file)) {
                return new CompressedMemory(sessionId);
            }
            try {
                CompressedMemory memory = objectMapper.readValue(file.toFile(), CompressedMemory.class);
                if (memory.getSessionId() == null || memory.getSessionId().isBlank()) {
                    memory.setSessionId(sessionId);
                }
                log.info("Loaded existing compressed memory for session {}, {} chunks",
                        sessionId, memory.getChunks().size());
                return memory;
            } catch (Exception e) {
                log.warn("Failed to load compressed memory JSON, creating new one: {}", e.getMessage());
                return new CompressedMemory(sessionId);
            }
        }
    }

    private void appendMessages(String sessionId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>();
            for (Message message : messages) {
                lines.add(objectMapper.writeValueAsString(StoredMessage.from(message)));
            }
            Files.write(sessionFile(sessionId), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("Failed to append session memory: {}", e.getMessage(), e);
        }
    }

    private int countStoredMessages(String sessionId) {
        return readStoredMessageLines(sessionId).size();
    }

    private List<String> readStoredMessageLines(String sessionId) {
        Path file = sessionFile(sessionId);
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .filter(line -> line != null && !line.isBlank())
                    .toList();
        } catch (IOException e) {
            log.error("Failed to read session memory lines: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void rewriteSessionLines(String sessionId, List<String> lines) {
        try {
            Path target = sessionFile(sessionId);
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            moveAtomically(temp, target);
        } catch (Exception e) {
            log.error("Failed to rewrite session memory: {}", e.getMessage(), e);
        }
    }

    private Message toMessage(StoredMessage storedMessage) {
        if ("assistant".equalsIgnoreCase(storedMessage.getRole())) {
            return new AssistantMessage(storedMessage.getText());
        }
        return new UserMessage(storedMessage.getText());
    }

    private void writeJsonAtomically(Path target, Object value) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        objectMapper.writeValue(temp.toFile(), value);
        moveAtomically(temp, target);
    }

    private void moveAtomically(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(sessionsDir);
            Files.createDirectories(summariesDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create memory directories", e);
        }
    }

    private Path sessionFile(String sessionId) {
        return sessionsDir.resolve(safeFileName(sessionId) + ".jsonl");
    }

    private Path summaryFile(String sessionId) {
        return summariesDir.resolve(safeFileName(sessionId) + ".json");
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Object lockFor(String key) {
        return locks.computeIfAbsent(safeFileName(key), ignored -> new Object());
    }

    private static class StoredMessage {
        private String role;
        private String text;
        private long timestamp;

        public StoredMessage() {
        }

        private StoredMessage(String role, String text, long timestamp) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
        }

        private static StoredMessage from(Message message) {
            String role = message.getMessageType().name().toLowerCase();
            return new StoredMessage(role, message.getText(), System.currentTimeMillis());
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
