package com.blog.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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

        this.url      = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");

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
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * INITIALIZE THE DATABASE SCHEMA
     * Creates the articles table if it doesn't already exist.
     * In Spring, this was handled by Hibernate with ddl-auto=update.
     * Here we do it ourselves with plain SQL.
     */
        public void initializeSchema() {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS articles (
                    id         BIGSERIAL PRIMARY KEY,
                    title      VARCHAR(500) NOT NULL,
                    content    TEXT NOT NULL,
                    date       DATE NOT NULL,
                    published  BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
                )
                """;

            // try-with-resources: automatically closes connection + statement when done
            try (Connection conn = getConnection();
                 var stmt = conn.createStatement()) {

                stmt.execute(createTableSQL);
                System.out.println("✅ Database schema initialized (table 'articles' ready).");

            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize database schema", e);
            }
            String createUserTableSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    id         BIGSERIAL PRIMARY KEY,
                    username   VARCHAR(100) NOT NULL UNIQUE,
                    password   VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT NOW()
                )
                """;
        }

        /**
         * SEED INITIAL DATA
         * Only inserts sample articles if the table is empty.
         * Same logic as DataSeeder.java in the Spring version.
         */
        public void seedIfEmpty() {
            String countSQL  = "SELECT COUNT(*) FROM articles";
            String insertSQL = """
                INSERT INTO articles (title, content, date, published)
                VALUES (?, ?, ?, ?)
                """;

            try (Connection conn = getConnection()) {

                // Check how many rows are in the table
                long count;
                try (var stmt = conn.createStatement();
                     var rs = stmt.executeQuery(countSQL)) {
                    rs.next();
                    count = rs.getLong(1);
                }

                if (count > 0) {
                    System.out.println("ℹ️  Database already has data, skipping seed.");
                    return;
                }

                // Insert 3 sample articles using PreparedStatement
                // PreparedStatement = safe SQL with placeholders (?) — prevents SQL injection
                try (var ps = conn.prepareStatement(insertSQL)) {

                    // Article 1
                    ps.setString(1, "Getting Started with React");
                    ps.setString(2, "React is a powerful JavaScript library for building user interfaces. " +
                            "In this article, we'll explore the fundamentals of React and how to get started.\n\n" +
                            "React uses a component-based architecture, making it easy to build and maintain " +
                            "complex applications. Components are reusable pieces of UI that can manage their own state.\n\n" +
                            "Whether you're building a simple website or a complex web application, " +
                            "React provides the tools and patterns you need to succeed.");
                    ps.setDate(3, java.sql.Date.valueOf("2024-01-15"));
                    ps.setBoolean(4, true);
                    ps.executeUpdate(); // runs the INSERT for article 1

                    // Article 2
                    ps.setString(1, "Understanding Modern CSS");
                    ps.setString(2, "CSS has evolved significantly over the years. Modern CSS includes features " +
                            "like Grid, Flexbox, and custom properties that make styling web applications much easier.\n\n" +
                            "These tools allow developers to create responsive, beautiful layouts with less code. " +
                            "Grid and Flexbox have revolutionized how we approach layout design.\n\n" +
                            "In this article, we'll dive deep into these modern CSS features.");
                    ps.setDate(3, java.sql.Date.valueOf("2024-01-10"));
                    ps.setBoolean(4, true);
                    ps.executeUpdate();

                    // Article 3
                    ps.setString(1, "JavaScript ES6+ Features");
                    ps.setString(2, "JavaScript has come a long way with ES6 and beyond. Arrow functions, " +
                            "destructuring, spread operators, and async/await have transformed how we write JS.\n\n" +
                            "These features not only make code more concise but also more readable and maintainable. " +
                            "Understanding these modern JavaScript features is essential for any web developer today.");
                    ps.setDate(3, java.sql.Date.valueOf("2024-01-05"));
                    ps.setBoolean(4, true);
                    ps.executeUpdate();
                }

                System.out.println("✅ Sample articles inserted into database.");

            } catch (SQLException e) {
                throw new RuntimeException("Failed to seed database", e);
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