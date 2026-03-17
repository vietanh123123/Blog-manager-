package com.blog.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * DATABASE CONNECTION MANAGER
 *
 * WHAT DID SPRING DO?
 * In the Spring version, you just put these lines in application.properties:
 *   spring.datasource.url=...
 *   spring.datasource.username=...
 *   spring.datasource.password=...
 * And Spring Boot automatically created a connection pool (HikariCP) for you.
 *
 * WHAT WE DO NOW:
 * We manage the database connection ourselves using plain JDBC.
 * JDBC = Java Database Connectivity — the standard Java API for talking to databases.
 *
 * HOW A DATABASE CONNECTION WORKS:
 * Think of it like a phone call:
 *   - DriverManager.getConnection() = dialing the number (opening the connection)
 *   - Running SQL = having the conversation
 *   - connection.close() = hanging up (must always do this to free resources!)
 *
 * WHY A SINGLETON?
 * We only want ONE DatabaseManager instance in the whole app.
 * The Singleton pattern ensures that.
 */
public class DatabaseManager {

    // The single instance of this class (Singleton pattern)
    private static DatabaseManager instance;

    private final String url;
    private final String username;
    private final String password;

    /**
     * Private constructor — prevents anyone from doing `new DatabaseManager()`.
     * Loads config from db.properties file.
     */
    private DatabaseManager() {
        Properties props = new Properties();

        // Load the properties file from the classpath (src/ folder)
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException("db.properties file not found in src/ folder!");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }

        String resolvedUrl = resolveConfigValue(props, "db.url", "DB_URL", "DATABASE_URL");
        resolvedUrl = normalizeJdbcUrl(resolvedUrl);

        String resolvedUsername = resolveConfigValue(props, "db.username", "DB_USERNAME", "PGUSER", "POSTGRES_USER");
        String resolvedPassword = resolveConfigValue(props, "db.password", "DB_PASSWORD", "PGPASSWORD", "POSTGRES_PASSWORD");

        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            throw new RuntimeException("Database URL is missing. Set db.url in db.properties or env DB_URL / DATABASE_URL.");
        }

        this.url = resolvedUrl;
        this.username = resolvedUsername;
        this.password = resolvedPassword;

        // Load the PostgreSQL JDBC driver class into memory.
        // In Spring, this happened automatically. Here we do it explicitly.
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ PostgreSQL JDBC driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "PostgreSQL JDBC driver not found! Make sure postgresql-*.jar is in the lib/ folder.", e
            );
        }
    }

    private String resolveConfigValue(Properties props, String key, String... envFallbackKeys) {
        String value = props.getProperty(key);
        if (value != null) {
            value = value.trim();
        }

        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String envKey = value.substring(2, value.length() - 1).trim();
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.isBlank()) {
                return envValue.trim();
            }
            value = null;
        }

        if (value != null && !value.isBlank()) {
            return value;
        }

        Map<String, String> env = System.getenv();
        for (String envKey : envFallbackKeys) {
            String envValue = env.get(envKey);
            if (envValue != null && !envValue.isBlank()) {
                return envValue.trim();
            }
        }

        return null;
    }

    private String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }

        String url = rawUrl.trim();
        if (url.startsWith("jdbc:postgresql://")) {
            return url;
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        return url;
    }

    /**
     * SINGLETON ACCESSOR
     * Returns the single DatabaseManager instance.
     * Creates it on first call (lazy initialization).
     *
     * `synchronized` prevents two threads from creating two instances simultaneously.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * OPEN A NEW DATABASE CONNECTION
     *
     * This is what you call every time you need to run a SQL query.
     * Each call opens a fresh connection to PostgreSQL.
     *
     * IMPORTANT: Always close the connection after use!
     * We do this with try-with-resources in the Repository:
     *   try (Connection conn = DatabaseManager.getInstance().getConnection()) {
     *       // use conn here — auto-closed when block exits
     *   }
     *
     * @throws SQLException if connection fails (wrong password, DB not running, etc.)
     */
    public Connection getConnection() throws SQLException {
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * INITIALIZE THE DATABASE SCHEMA
     * Creates the articles and users tables if they don't already exist.
     * In Spring, this was handled by Hibernate with ddl-auto=update.
     * Here we do it ourselves with plain SQL.
     */
    public void initializeSchema() {
        String createUserTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id         BIGSERIAL PRIMARY KEY,
                username   VARCHAR(100) NOT NULL UNIQUE,
                password   VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT NOW()
            )
            """;

        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS articles (
                id         BIGSERIAL PRIMARY KEY,
                title      VARCHAR(500) NOT NULL,
                content    TEXT NOT NULL,
                date       DATE NOT NULL,
                published  BOOLEAN NOT NULL DEFAULT TRUE,
                user_id    BIGINT REFERENCES users(id) ON DELETE CASCADE,
                created_at TIMESTAMP DEFAULT NOW(),
                updated_at TIMESTAMP DEFAULT NOW()
            )
            """;

        String renameLegacyUserColumnSQL = """
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'articles'
                      AND column_name = 'userid'
                ) AND NOT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'articles'
                      AND column_name = 'user_id'
                ) THEN
                    EXECUTE 'ALTER TABLE articles RENAME COLUMN userid TO user_id';
                END IF;
            END
            $$;
            """;

        String ensureUserColumnSQL = """
            ALTER TABLE articles
            ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id) ON DELETE CASCADE
            """;

        // try-with-resources: automatically closes connection + statement when done
        try (Connection conn = getConnection();
             var stmt = conn.createStatement()) {

            stmt.execute(createUserTableSQL);
            stmt.execute(createTableSQL);
            stmt.execute(renameLegacyUserColumnSQL);
            stmt.execute(ensureUserColumnSQL);
            System.out.println("✅ Database schema initialized (tables 'articles' and 'users' ready).");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public void seedAdminUsers() {
        String countSQL = "SELECT COUNT(*) FROM users";
        String insertSQL = """
                INSERT INTO users (username, password)
                VALUES (?, ?)
                """;
        // In a real app, you'd hash the password before storing it!
        // Insert a default admin user: username=admin, password=admin123
        try (Connection conn = getConnection()) {

            // Check if users table is empty
            long count;
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(countSQL)) {
                rs.next();
                count = rs.getLong(1);
            }

            if (count > 0) {
                System.out.println("ℹ️  Users table already has data, skipping admin seed.");
                return;
            }

            // Insert default admin user
            try (var ps = conn.prepareStatement(insertSQL)) {
                ps.setString(1, "admin");
                ps.setString(2, "admin123"); // In production, hash this password!
                ps.executeUpdate();
            }

            System.out.println("✅ Default admin user inserted into database.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed admin user", e);
        }
    }
}