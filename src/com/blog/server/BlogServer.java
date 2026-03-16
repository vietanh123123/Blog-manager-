package com.blog.server;

import com.blog.config.DatabaseManager;
import com.blog.controller.ArticleController;
import com.sun.net.httpserver.HttpServer;
import com.blog.controller.AuthController;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**— MAIN ENTRY POINT
 *
 * BLOG SERVER
 * WHAT DID SPRING BOOT DO?
 * Running `mvn spring-boot:run` triggered SpringApplication.run(), which:
 *   1. Started an embedded Tomcat server on port 8080
 *   2. Scanned for @RestController, @Service, @Repository classes
 *   3. Created instances of all those classes and wired them together (DI)
 *   4. Registered all @GetMapping/@PostMapping routes automatically
 *   5. Connected to the database using your application.properties
 *   6. Created/updated database tables (ddl-auto=update)
 *
 * WHAT WE DO NOW — EVERYTHING MANUALLY:
 *   1. Start Java's built-in HttpServer on port 8080
 *   2. Create instances of our classes ourselves (no DI framework)
 *   3. Register routes explicitly by calling controller.registerRoutes(router)
 *   4. Connect to the database using our DatabaseManager
 *   5. Create the database table and seed data ourselves
 *
 * JAVA'S BUILT-IN HTTP SERVER (com.sun.net.httpserver):
 * This is part of the JDK itself — no dependencies needed!
 * It's not as powerful as Tomcat/Jetty, but perfectly fine for learning
 * and for moderate traffic applications.
 *
 * HOW TO RUN:
 *   javac -cp lib/postgresql-42.7.3.jar -d out $(find src -name "*.java")
 *   java -cp out:lib/postgresql-42.7.3.jar com.blog.server.BlogServer
 *
 * Or use the provided build scripts:
 *   Linux/Mac: ./run.sh
 *   Windows:   run.bat
 */
public class BlogServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        System.out.println("═══════════════════════════════════");
        System.out.println("   Blog Backend (Plain Java Mode)  ");
        System.out.println("═══════════════════════════════════");

        // ── STEP 1: Initialize Database ──────────────────────────
        // In Spring, this happened automatically via application.properties +
        // Hibernate's ddl-auto=update + DataSeeder's CommandLineRunner.
        // Here we call it explicitly.
        System.out.println("\n📦 Initializing database...");
        DatabaseManager db = DatabaseManager.getInstance();
        db.initializeSchema(); // CREATE TABLE IF NOT EXISTS articles (...)

        // ── STEP 2: Create the Router ─────────────────────────────
        // The router is our replacement for Spring's dispatcher servlet
        // (the thing that looks at a URL and decides which controller handles it)
        Router router = new Router();

        // ── STEP 3: Register Routes ───────────────────────────────
        // In Spring, @RestController + @GetMapping did this automatically.
        // Here we call registerRoutes() explicitly for each controller.
        ArticleController articleController = new ArticleController();
        articleController.registerRoutes(router);

        AuthController authController = new AuthController();
        authController.registerRoutes(router);

        System.out.println("\n🗺️  Routes registered:");
        System.out.println("   GET    /api/articles/published");
        System.out.println("   GET    /api/articles");
        System.out.println("   GET    /api/articles/{id}");
        System.out.println("   POST   /api/articles");
        System.out.println("   POST   /api/auth/register");
        System.out.println("   POST   /api/auth/login");
        System.out.println("   PUT    /api/articles/{id}");
        System.out.println("   DELETE /api/articles/{id}");

        // ── STEP 4: Start the HTTP Server ─────────────────────────
        // HttpServer.create() sets up a TCP socket on the given port.
        // InetSocketAddress(PORT) = listen on all network interfaces, port 8080.
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Mount our router on the "/" path.
        // All requests to any URL will go through our router.
        // "For ALL incoming requests (no matter what path), send them to router"
        // Router implements HttpHandler, so it can be registered directly with the server.
        server.createContext("/", router);

        // Set a thread pool so the server can handle multiple requests concurrently.
        // newVirtualThreadPerTaskExecutor() = Java 21's virtual threads (lightweight!)
        // For Java 17, use: Executors.newFixedThreadPool(10) instead
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Start accepting connections
        server.start();

        System.out.println("\n✅ Blog backend is running!");
        System.out.println("   API:     http://localhost:" + PORT + "/api/articles");
        System.out.println("   Test:    http://localhost:" + PORT + "/api/articles/published");
        System.out.println("\n   Press Ctrl+C to stop the server.");
        System.out.println("═══════════════════════════════════\n");
    }
}