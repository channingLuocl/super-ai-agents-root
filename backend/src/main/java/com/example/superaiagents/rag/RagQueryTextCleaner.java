package com.example.superaiagents.rag;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class RagQueryTextCleaner {

    private static final Pattern THINK_BLOCK = Pattern.compile("(?is)<think>.*?</think>");
    private static final Pattern UNCLOSED_THINK_BLOCK = Pattern.compile("(?is)<think>.*");
    private static final List<String> STOP_MARKERS = List.of(
            "**Reasoning", "Reasoning:", "原因：", "原因:", "解释：", "解释:",
            "说明：", "说明:", "These expanded queries", "These queries"
    );

    private RagQueryTextCleaner() {
    }

    static String cleanSingleQuery(String rawText, String fallback) {
        String cleaned = normalize(rawText);
        String labeled = extractAfterLabel(cleaned, List.of(
                "Rewritten query", "Rewritten Query", "改写查询", "重写查询", "检索查询", "查询"
        ));
        if (StringUtils.hasText(labeled)) {
            cleaned = labeled;
        }

        String query = firstUsefulLine(cutBeforeStopSection(cleaned));
        query = cleanCandidate(query);
        if (query.contains("|")) {
            query = query.split("\\|", 2)[0].trim();
        }
        return StringUtils.hasText(query) ? query : cleanCandidate(fallback);
    }

    static String cleanExpandedQueries(String rawText, String originalQuery) {
        String cleaned = normalize(rawText);
        String labeled = extractAfterLabel(cleaned, List.of(
                "Expanded queries", "Expanded Queries", "扩展查询", "扩展词"
        ));
        if (StringUtils.hasText(labeled)) {
            cleaned = labeled;
        }
        cleaned = cutBeforeStopSection(cleaned);

        Set<String> queries = new LinkedHashSet<>();
        addIfPresent(queries, cleanCandidate(originalQuery));
        for (String part : splitCandidates(cleaned)) {
            addIfPresent(queries, cleanCandidate(part));
            if (queries.size() >= 6) {
                break;
            }
        }

        return queries.isEmpty() ? cleanCandidate(originalQuery) : String.join(" | ", queries);
    }

    private static String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        String text = rawText.replace("\r\n", "\n").replace('\r', '\n');
        text = THINK_BLOCK.matcher(text).replaceAll("\n");
        text = UNCLOSED_THINK_BLOCK.matcher(text).replaceAll("\n");
        text = text.replaceAll("(?is)```.*?```", "\n");
        return text.trim();
    }

    private static String extractAfterLabel(String text, List<String> labels) {
        for (String label : labels) {
            Pattern pattern = Pattern.compile("(?is)(?:\\*\\*)?" + Pattern.quote(label)
                    + "(?:\\*\\*)?\\s*[:：]?\\s*(.+)");
            var matcher = pattern.matcher(text);
            if (matcher.find()) {
                String candidate = firstUsefulLine(cutBeforeStopSection(matcher.group(1)));
                if (StringUtils.hasText(candidate)) {
                    return candidate;
                }
            }
        }
        return "";
    }

    private static String cutBeforeStopSection(String text) {
        String result = text;
        for (String marker : STOP_MARKERS) {
            int index = result.indexOf(marker);
            if (index >= 0) {
                result = result.substring(0, index);
            }
        }
        return result;
    }

    private static String firstUsefulLine(String text) {
        for (String line : text.split("\\n")) {
            String candidate = cleanCandidate(line);
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            String lower = candidate.toLowerCase();
            if (lower.contains("reasoning") || lower.contains("expanded queries")
                    || lower.contains("rewritten query")) {
                continue;
            }
            return candidate;
        }
        return "";
    }

    private static List<String> splitCandidates(String text) {
        List<String> candidates = new ArrayList<>();
        for (String part : text.split("[|｜\\n；;]+")) {
            String cleaned = cleanCandidate(part);
            if (StringUtils.hasText(cleaned)) {
                candidates.add(cleaned);
            }
        }
        return candidates;
    }

    private static String cleanCandidate(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim()
                .replace("**", "")
                .replace("`", "")
                .replaceAll("^#+\\s*", "")
                .replaceAll("^[-*+\\d.、)）\\s]+", "")
                .replaceAll("^[\"'“”]+|[\"'“”]+$", "")
                .trim();

        cleaned = cleaned.replaceAll("(?i)^query\\s*[:：]\\s*", "")
                .replaceAll("^(查询|检索查询|改写查询|重写查询|扩展查询)\\s*[:：]\\s*", "")
                .trim();

        int explanationIndex = cleaned.indexOf(" - ");
        if (explanationIndex > 0) {
            cleaned = cleaned.substring(0, explanationIndex).trim();
        }
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static void addIfPresent(Set<String> queries, String query) {
        if (StringUtils.hasText(query)) {
            queries.add(query);
        }
    }
}
