package com.blog.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WHAT IS THIS?
 * A plain Java class (also called a POJO — Plain Old Java Object) that represents
 * one row in the "articles" table in PostgreSQL.
 *
 * WITHOUT SPRING/JPA:
 * There are no magic annotations like @Entity or @Column.
 * This is just a regular class with fields, getters, and setters.
 * We manually map database rows → Article objects in the Repository layer.
 *
 * DATABASE TABLE (you create this manually — see README):
 *
// *   CREATE TABLE articles (
 *       id         BIGSERIAL PRIMARY KEY,
 *       title      VARCHAR(500) NOT NULL,
 *       content    TEXT NOT NULL,
 *       date       DATE NOT NULL,
 *       published  BOOLEAN NOT NULL DEFAULT TRUE,
 *       created_at TIMESTAMP DEFAULT NOW(),
 *       updated_at TIMESTAMP DEFAULT NOW()
 *   );
 */
public class Article {

    // These fields map 1-to-1 with columns in the articles table
    private Long id;
    private String title;
    private String content;
    private LocalDate date;
    private boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Constructors ---

    public Article() {}

    public Article(Long id, String title, String content, LocalDate date,
                   boolean published, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- Getters & Setters ---
    // Without Lombok (which was a Spring-world shortcut), we write these manually.

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Article{id=" + id + ", title='" + title + "', date=" + date + ", published=" + published + "}";
    }
}