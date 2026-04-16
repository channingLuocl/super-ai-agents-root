package com.example.superaiagents.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 Markdown RAG 文档增量同步到 Redis VectorStore。
 */
@Component
@Slf4j
public class RagIndexSynchronizer {

    @Resource
    private FoodDocumentLoader foodDocumentLoader;

    @Resource(name = "redisVectorStore")
    private VectorStore vectorStore;

    @Resource
    private JedisPooled jedisPooled;

    @Value("${rag.index.sync-on-startup:true}")
    private boolean syncOnStartup;

    @Value("${rag.index.reset-on-missing-manifest:true}")
    private boolean resetOnMissingManifest;

    @Value("${rag.index.batch-size:10}")
    private int batchSize;

    @Value("${spring.ai.vectorstore.redis.prefix:doc:}")
    private String redisPrefix;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path manifestPath = Path.of(System.getProperty("user.dir"), "memory", "rag-index-manifest.json");

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!syncOnStartup) {
            log.info("RAG 索引启动同步已关闭");
            return;
        }
        sync();
    }

    public synchronized void sync() {
        try {
            Files.createDirectories(manifestPath.getParent());
            boolean manifestExists = Files.exists(manifestPath);
            if (!manifestExists && resetOnMissingManifest) {
                clearRedisVectorKeys();
            }

            RagIndexManifest previousManifest = manifestExists ? readManifest() : new RagIndexManifest();
            Map<String, RagIndexManifest.RagIndexManifestEntry> previousById = toEntryMap(previousManifest);

            List<Document> currentDocuments = foodDocumentLoader.loadMarkdowns().stream()
                    .map(this::withStableId)
                    .toList();
            Map<String, Document> currentById = new LinkedHashMap<>();
            for (Document document : currentDocuments) {
                currentById.put(document.getId(), document);
            }

            List<String> idsToDelete = previousById.keySet().stream()
                    .filter(id -> !currentById.containsKey(id))
                    .toList();
            if (!idsToDelete.isEmpty()) {
                vectorStore.delete(idsToDelete);
            }

            List<Document> documentsToAdd = new ArrayList<>();
            List<RagIndexManifest.RagIndexManifestEntry> nextEntries = new ArrayList<>();
            for (Document document : currentDocuments) {
                RagIndexManifest.RagIndexManifestEntry nextEntry = toEntry(document);
                RagIndexManifest.RagIndexManifestEntry previousEntry = previousById.get(nextEntry.getId());
                if (previousEntry == null || !nextEntry.getContentHash().equals(previousEntry.getContentHash())) {
                    if (previousEntry != null) {
                        vectorStore.delete(List.of(nextEntry.getId()));
                    }
                    documentsToAdd.add(document);
                } else {
                    nextEntry.setUpdatedAt(previousEntry.getUpdatedAt());
                }
                nextEntries.add(nextEntry);
            }

            addInBatches(documentsToAdd);

            RagIndexManifest nextManifest = new RagIndexManifest();
            nextManifest.setEntries(nextEntries);
            writeManifest(nextManifest);

            log.info("RAG 索引同步完成: 当前 {}, 新增/更新 {}, 删除 {}",
                    currentDocuments.size(), documentsToAdd.size(), idsToDelete.size());
        } catch (Exception e) {
            log.error("RAG 索引同步失败", e);
        }
    }

    private Document withStableId(Document document) {
        String filename = metadataAsString(document, "filename");
        String title = metadataAsString(document, "title");
        String contentHash = contentHash(document);
        String idSeed = filename + "::" + title + "::" + contentHash;
        String id = DigestUtils.md5DigestAsHex(idSeed.getBytes(StandardCharsets.UTF_8));
        return document.mutate()
                .id(id)
                .metadata("contentHash", contentHash)
                .build();
    }

    private String contentHash(Document document) {
        return DigestUtils.md5DigestAsHex(document.getText().getBytes(StandardCharsets.UTF_8));
    }

    private RagIndexManifest readManifest() throws IOException {
        return objectMapper.readValue(manifestPath.toFile(), RagIndexManifest.class);
    }

    private void writeManifest(RagIndexManifest manifest) throws IOException {
        objectMapper.writeValue(manifestPath.toFile(), manifest);
    }

    private Map<String, RagIndexManifest.RagIndexManifestEntry> toEntryMap(RagIndexManifest manifest) {
        Map<String, RagIndexManifest.RagIndexManifestEntry> entries = new LinkedHashMap<>();
        for (RagIndexManifest.RagIndexManifestEntry entry : manifest.getEntries()) {
            if (entry.getId() != null && !entry.getId().isBlank()) {
                entries.put(entry.getId(), entry);
            }
        }
        return entries;
    }

    private RagIndexManifest.RagIndexManifestEntry toEntry(Document document) {
        RagIndexManifest.RagIndexManifestEntry entry = new RagIndexManifest.RagIndexManifestEntry();
        entry.setId(document.getId());
        entry.setContentHash(metadataAsString(document, "contentHash"));
        entry.setFilename(metadataAsString(document, "filename"));
        entry.setTitle(metadataAsString(document, "title"));
        entry.setSource(metadataAsString(document, "source"));
        entry.setCategory(metadataAsString(document, "category"));
        entry.setImageUrls(metadataAsStringList(document, "imageUrls"));
        entry.setUpdatedAt(Instant.now().toString());
        return entry;
    }

    private void addInBatches(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }
        int size = Math.max(1, batchSize);
        for (int i = 0; i < documents.size(); i += size) {
            int end = Math.min(i + size, documents.size());
            vectorStore.add(documents.subList(i, end));
            log.info("RAG 文档已写入向量库: {}-{} / {}", i + 1, end, documents.size());
        }
    }

    private void clearRedisVectorKeys() {
        String pattern = redisPrefix + "*";
        List<String> keys = new ArrayList<>(jedisPooled.keys(pattern));
        if (keys.isEmpty()) {
            return;
        }
        jedisPooled.del(keys.toArray(String[]::new));
        log.info("manifest 不存在，已清理 Redis RAG 向量 key: {}", keys.size());
    }

    private String metadataAsString(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> metadataAsStringList(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof String string && !string.isBlank()) {
            return List.of(string);
        }
        return List.of();
    }
}
