package com.blog.util;

import com.blog.model.Article;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * JSON UTILITY — NO JACKSON, NO GSON
 *
 * WHAT DID SPRING DO?
 * Spring Boot includes Jackson by default. When a controller method returned
 * an object, Jackson automatically converted it to a JSON string.
 * e.g. Article → {"id":1,"title":"React","content":"..."}
 *
 * WHAT WE DO NOW:
 * We write our own JSON serialization (Java → JSON string)
 * and deserialization (JSON string → Java values) from scratch.
 *
 * This is intentionally simple — it handles the exact JSON shapes
 * our blog API needs. A production app would use Jackson or Gson.
 *
 * KEY CONCEPT — WHAT IS JSON?
 * JSON = JavaScript Object Notation. A text format for data.
 * Rules:
 *   - Objects: { "key": value }
 *   - Arrays:  [ value1, value2 ]
 *   - Strings: "hello"  (always double quotes)
 *   - Numbers: 42
 *   - Booleans: true / false
 *   - Null: null
 */
public class JsonUtil {

    // ============================================================
    // SERIALIZATION: Java Objects → JSON Strings
    // ============================================================

    /**
     * Convert a single Article to a JSON string.
     *
     * Output example:
     * {
     *   "id": 1,
     *   "title": "Getting Started with React",
     *   "content": "React is a powerful...",
     *   "date": "2024-01-15",
     *   "published": true
     * }
     */
    public static String articleToJson(Article article) {
        return "{" +
                "\"id\":" + (article.getId() == null ? "null" : article.getId()) + "," +
                "\"title\":" + escapeString(article.getTitle()) + "," +
                "\"content\":" + escapeString(article.getContent()) + "," +
                "\"date\":\"" + article.getDate() + "\"," +
                "\"published\":" + article.isPublished() +
                "}";
    }

    /**
     * Convert a List of Articles to a JSON array string.
     *
     * Output example:
     * [{"id":1,"title":"..."}, {"id":2,"title":"..."}]
     */
    public static String articlesToJson(List<Article> articles) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < articles.size(); i++) {
            sb.append(articleToJson(articles.get(i)));
            if (i < articles.size() - 1) {
                sb.append(","); // comma between items, NOT after the last one
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Build a simple error JSON response.
     *
     * Output: {"error": "Article not found with id: 99"}
     */
    public static String errorJson(String message) {
        return "{\"error\":" + escapeString(message) + "}";
    }

    /**
     * Build a simple message JSON response.
     *
     * Output: {"message": "Article deleted successfully"}
     */
    public static String messageJson(String message) {
        return "{\"message\":" + escapeString(message) + "}";
    }

    /**
     * ESCAPE a Java String for safe use inside JSON.
     */
    public static String escapeString(String value) {
        if (value == null) return "null";
        String escaped = value
                .replace("\\", "\\\\")   // backslash first (order matters!)
                .replace("\"", "\\\"")   // double quotes
                .replace("\n", "\\n")    // newlines
                .replace("\r", "\\r")    // carriage returns
                .replace("\t", "\\t");   // tabs
        return "\"" + escaped + "\"";
    }

    // ============================================================
    // DESERIALIZATION: JSON String → Java values
    // ============================================================

    /**
     * Parse a JSON request body into a Map<String, String>.
     * This simple parser now tolerates a leading UTF-8 BOM and extra whitespace.
     */
    public static Map<String, String> parseJsonBody(String json) {
        Map<String, String> result = new HashMap<>();

        if (json == null || json.isBlank()) return result;

        // Remove UTF-8 BOM if present
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }

        // Remove outer whitespace and the surrounding { }
        json = json.strip();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))  json = json.substring(0, json.length() - 1);

        // Split into key:value pairs
        String[] pairs = splitJsonPairs(json);

        for (String pair : pairs) {
            pair = pair.strip();
            if (pair.isEmpty()) continue;

            int colonIndex = pair.indexOf(':');
            if (colonIndex == -1) continue;

            String key   = unquote(pair.substring(0, colonIndex).strip());
            String value = unquote(pair.substring(colonIndex + 1).strip());

            result.put(key, value);
        }

        return result;
    }

    /**
     * Extract a specific field from a raw JSON string without full parsing.
     */
    public static String extractField(String json, String fieldName) {
        Map<String, String> parsed = parseJsonBody(json);
        return parsed.get(fieldName);
    }

    /**
     * Convert a field value string to boolean.
     */
    public static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private static String unquote(String value) {
        if (value == null) return null;
        value = value.strip();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            // Unescape common sequences
            value = value
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return value;
    }

    private static String[] splitJsonPairs(String json) {
        List<String> pairs = new java.util.ArrayList<>();
        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString; // toggle in/out of string
            }

            if (!inString && c == '{') depth++;
            if (!inString && c == '}') depth--;

            // Only split on commas that are at depth 0 and outside strings
            if (!inString && depth == 0 && c == ',') {
                pairs.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            pairs.add(current.toString());
        }

        return pairs.toArray(new String[0]);
    }
}