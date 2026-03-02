package com.blog.repository;

import com.blog.config.DatabaseManager;
import com.blog.model.Article;
import com.blog.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ARTICLE REPOSITORY — RAW JDBC
 *
 * WHAT DID SPRING/JPA DO?
 * In the Spring version, extending JpaRepository gave us save(), findById(),
 * findAll(), deleteById() etc. for free — zero SQL written by us.
 *
 * WHAT WE DO NOW:
 * We write every SQL query ourselves. This is raw JDBC:
 *   1. Get a Connection from DatabaseManager
 *   2. Create a PreparedStatement with a SQL string
 *   3. Set the ? parameters
 *   4. Execute the query
 *   5. Read the ResultSet (for SELECT queries) row by row
 *   6. Map each row to an Article object
 *   7. Close everything (handled by try-with-resources)
 *
 * KEY JDBC TYPES:
 *   Connection        → the live link to the database (like a phone line)
 *   PreparedStatement → a SQL query with ? placeholders (safe from SQL injection)
 *   ResultSet         → the rows returned by a SELECT query (like a cursor you move through)
 *
 * WHY PreparedStatement INSTEAD OF Statement?
 * NEVER do this:  "SELECT * FROM articles WHERE id = " + id
 * This allows SQL injection attacks! (e.g. id = "1; DROP TABLE articles;")
 *
 * DO THIS instead: "SELECT * FROM articles WHERE id = ?"
 * Then: ps.setLong(1, id)  — JDBC safely escapes the value.
 */
public class ArticleRepository {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    /**
     * GET ALL PUBLISHED ARTICLES (for public home page)
     *
     * SQL: SELECT * FROM articles WHERE published = TRUE ORDER BY date DESC
     *
     * ResultSet navigation:
     *   rs.next()       → moves cursor to next row, returns false when no more rows
     *   rs.getLong("id") → reads the "id" column from current row as a Long
     */
    public List<Article> findAllPublished() throws SQLException {
        String sql = "SELECT * FROM articles WHERE published = TRUE ORDER BY date DESC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return mapResultSetToList(rs);
        }
    }

    /**
     * GET ALL ARTICLES (for admin dashboard — includes drafts)
     *
     * SQL: SELECT * FROM articles ORDER BY date DESC
     */
    public List<Article> findAll() throws SQLException {
        String sql = "SELECT * FROM articles ORDER BY date DESC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return mapResultSetToList(rs);
        }
    }

    /**
     * GET SINGLE ARTICLE BY ID
     *
     * SQL: SELECT * FROM articles WHERE id = ?
     *
     * Returns Optional<Article>:
     *   Optional is a Java wrapper that means "this might or might not have a value".
     *   Optional.of(article)    → found it
     *   Optional.empty()        → no article with that ID
     *
     * This is better than returning null, which causes NullPointerExceptions.
     */
    public Optional<Article> findById(long id) throws SQLException {
        String sql = "SELECT * FROM articles WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id); // replace first ? with the id value

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs)); // found → wrap in Optional
                }
                return Optional.empty(); // not found → empty Optional
            }
        }
    }

    // ============================================================
    // WRITE OPERATIONS
    // ============================================================

    /**
     * INSERT A NEW ARTICLE
     *
     * SQL: INSERT INTO articles (title, content, date, published)
     *      VALUES (?, ?, ?, ?)
     *      RETURNING id
     *
     * RETURNING id → PostgreSQL gives us back the auto-generated ID
     * (BIGSERIAL generates it automatically — we don't set it ourselves)
     *
     * We set the generated ID back on the article object before returning it.
     */
    public Article save(Article article) throws SQLException {
        String sql = """
            INSERT INTO articles (title, content, date, published)
            VALUES (?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, article.getTitle());
            ps.setString(2, article.getContent());
            ps.setDate(3, Date.valueOf(article.getDate())); // LocalDate → java.sql.Date
            ps.setBoolean(4, article.isPublished());

            // executeQuery() for INSERT ... RETURNING (it returns rows)
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                // Set the database-generated values back on the object
                article.setId(rs.getLong("id"));
                java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    article.setCreatedAt(createdAt.toLocalDateTime());
                } else {
                    article.setCreatedAt(null);
                }
               java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    article.setUpdatedAt(updatedAt.toLocalDateTime());
                } else {
                    article.setUpdatedAt(null);
                }

            }

            return article;
        }
    }

    /**
     * UPDATE AN EXISTING ARTICLE
     *
     * SQL: UPDATE articles
     *      SET title = ?, content = ?, date = ?, published = ?, updated_at = NOW()
     *      WHERE id = ?
     *
     * Returns true if a row was updated, false if no article with that ID existed.
     * executeUpdate() returns the number of rows affected — 0 means nothing was updated.
     */
    public boolean update(long id, Article article) throws SQLException {
        String sql = """
            UPDATE articles
            SET title = ?, content = ?, date = ?, published = ?, updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, article.getTitle());
            ps.setString(2, article.getContent());
            ps.setDate(3, Date.valueOf(article.getDate()));
            ps.setBoolean(4, article.isPublished());
            ps.setLong(5, id); // the WHERE clause

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0; // true = successfully updated
        }
    }

    /**
     * DELETE AN ARTICLE BY ID
     *
     * SQL: DELETE FROM articles WHERE id = ?
     *
     * Returns true if a row was deleted, false if no article with that ID existed.
     */
    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM articles WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * MAP A SINGLE ResultSet ROW → Article OBJECT
     *
     * This is what JPA/Hibernate did automatically with @Entity.
     * Now we do it manually: read each column by name, set it on the Article.
     *
     * rs.getLong("id")           → reads the "id" column as a long
     * rs.getString("title")      → reads the "title" column as a String
     * rs.getDate("date")         → reads the "date" column as a java.sql.Date
     *   .toLocalDate()           → converts to Java's modern LocalDate type
     * rs.getTimestamp("created_at")
     *   .toLocalDateTime()       → reads TIMESTAMP column → LocalDateTime
     */
    private Article mapRow(ResultSet rs) throws SQLException {
        Article article = new Article();
        article.setId(rs.getLong("id"));
        article.setTitle(rs.getString("title"));
        article.setContent(rs.getString("content"));

        // Date might be null, handle safely
        Date date = rs.getDate("date");
        article.setDate(date != null ? date.toLocalDate() : null);

        article.setPublished(rs.getBoolean("published"));

        // Timestamps might be null if not set, so we check and explicitly set null
        Timestamp createdAt = rs.getTimestamp("created_at");
        article.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        article.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        return article;
    }

    /**
     * MAP ALL ROWS IN A ResultSet → List<Article>
     *
     * Loops through each row in the ResultSet, maps each to an Article,
     * and collects them into a List.
     */
    private List<Article> mapResultSetToList(ResultSet rs) throws SQLException {
        List<Article> articles = new ArrayList<>();
        while (rs.next()) { // rs.next() = "go to next row", returns false when done
            articles.add(mapRow(rs));
        }
        return articles;
    }

    public User arbitraryUser() {
        String sql = """
                SELECT username, password 
                FROM users
                ORDER BY RANDOM()
                LIMIT 1
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new User(rs.getString("username"), rs.getString("password"));
            } else {
                throw new RuntimeException("No users found in database");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user from database", e);
        }
    }
}
