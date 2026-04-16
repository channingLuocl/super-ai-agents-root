package com.example.superaiagents.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 向量索引清单，用于判断文档块是否需要重新 embedding。
 */
public class RagIndexManifest {

    private List<RagIndexManifestEntry> entries = new ArrayList<>();

    public List<RagIndexManifestEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<RagIndexManifestEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }

    public static class RagIndexManifestEntry {
        private String id;
        private String contentHash;
        private String filename;
        private String title;
        private String source;
        private String category;
        private List<String> imageUrls = new ArrayList<>();
        private String updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContentHash() {
            return contentHash;
        }

        public void setContentHash(String contentHash) {
            this.contentHash = contentHash;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }

        public void setImageUrls(List<String> imageUrls) {
            this.imageUrls = imageUrls == null ? new ArrayList<>() : imageUrls;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
