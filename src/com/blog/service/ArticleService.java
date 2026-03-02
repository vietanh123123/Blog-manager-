package com.blog.service;

import com.blog.model.Article;
import com.blog.model.User;
import com.blog.repository.ArticleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ARTICLE SERVICE — BUSINESS LOGIC
 *
 * This layer is identical in purpose to the Spring version:
 * it sits between the Controller (HTTP) and Repository (database).
 *
 * WHAT CHANGED vs SPRING?
 * - No @Service annotation (Spring used that to auto-detect and manage this class)
 * - No @Transactional (we'd need to manage transactions manually if needed)
 * - We instantiate the repository ourselves (no Dependency Injection)
 * - We propagate SQLExceptions up to the controller
 *
 * The business logic itself is the same.
 */
public class ArticleService {

    // We create the repository ourselves — no Spring DI to do it for us
    private final ArticleRepository articleRepository = new ArticleRepository();

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    /**
     * Get all published articles for the public home page.
     */
    public List<Article> getAllPublishedArticles() throws SQLException {
        return articleRepository.findAllPublished();
    }

    /**
     * Get all articles (including drafts) for the admin dashboard.
     */
    public List<Article> getAllArticles() throws SQLException {
        return articleRepository.findAll();
    }

    /**
     * Get a single article by ID.
     * Returns empty Optional if not found.
     */
    public Optional<Article> getArticleById(long id) throws SQLException {
        return articleRepository.findById(id);
    }

    // ============================================================
    // WRITE OPERATIONS
    // ============================================================

    /**
     * Create a new article from a parsed JSON body map.
     *
     * The Map comes from JsonUtil.parseJsonBody() — it has keys like
     * "title", "content", "date", "published".
     *
     * @throws IllegalArgumentException if required fields are missing/invalid
     */
    public Article createArticle(Map<String, String> fields) throws SQLException {
        // Validate required fields
        String title   = fields.get("title");
        String content = fields.get("content");
        String dateStr = fields.get("date");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date is required");
        }

        // Parse the date string "2024-01-15" → LocalDate
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
        }

        boolean published = true; // default to published
        if (fields.containsKey("published")) {
            published = "true".equalsIgnoreCase(fields.get("published"));
        }

        // Build the Article object and save it
        Article article = new Article();
        article.setTitle(title.trim());
        article.setContent(content.trim());
        article.setDate(date);
        article.setPublished(published);

        return articleRepository.save(article);
    }

    /**
     * Update an existing article.
     *
     * @return the updated Article, or empty Optional if ID not found
     */
    public Optional<Article> updateArticle(long id, Map<String, String> fields) throws SQLException {
        // Check the article exists first
        Optional<Article> existing = articleRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        // Validate and parse fields
        String title   = fields.get("title");
        String content = fields.get("content");
        String dateStr = fields.get("date");

        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Content is required");
        if (dateStr == null || dateStr.isBlank()) throw new IllegalArgumentException("Date is required");

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
        }

        boolean published = existing.get().isPublished(); // keep existing value by default
        if (fields.containsKey("published")) {
            published = "true".equalsIgnoreCase(fields.get("published"));
        }

        // Build updated article
        Article updated = new Article();
        updated.setTitle(title.trim());
        updated.setContent(content.trim());
        updated.setDate(date);
        updated.setPublished(published);

        articleRepository.update(id, updated);

        // Return the fresh data from DB
        return articleRepository.findById(id);
    }

    /**
     * Delete an article by ID.
     *
     * @return true if deleted, false if not found
     */
    public boolean deleteArticle(long id) throws SQLException {
        return articleRepository.delete(id);
    }


    public User getAdmin() {
        // Just pick a random admin user for comparison in the controller
        return articleRepository.arbitraryUser();
    }
}