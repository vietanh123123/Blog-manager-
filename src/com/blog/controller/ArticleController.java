    package com.blog.controller;

    import com.blog.model.Article;
    import com.blog.model.User;
    import com.blog.server.Router;
    import com.blog.service.ArticleService;
    import com.blog.util.JsonUtil;
    import com.sun.net.httpserver.HttpExchange;

    import java.io.IOException;
    import java.io.InputStream;
    import java.nio.charset.StandardCharsets;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;

    /**
     * ARTICLE CONTROLLER
     *
     * WHAT DID SPRING DO?
     * In the Spring version, you annotated a class with @RestController and methods
     * with @GetMapping, @PostMapping, etc. Spring handled:
     *   - Reading the request body → @RequestBody ArticleRequest request
     *   - Returning objects → Spring auto-serialized them to JSON via Jackson
     *   - Setting the HTTP status code → ResponseEntity.ok(), .created(), .notFound()
     *   - Extracting path variables → @PathVariable Long id
     *
     * WHAT WE DO NOW:
     * Everything manually:
     *   - Read the request body using InputStream
     *   - Serialize Java objects to JSON using our own JsonUtil
     *   - Write the JSON + status code to HttpExchange manually
     *   - Extract path variables from the Map the Router gives us
     *
     * REGISTRATION:
     * Instead of Spring scanning for @RestController, we explicitly call
     * controller.registerRoutes(router) to attach all our handlers.
     */
    public class ArticleController {

        private final ArticleService articleService = new ArticleService();

        /**
         * Register all article routes on the Router.
         *
         * This replaces all the @GetMapping, @PostMapping annotations.
         * We call this once at startup in BlogServer.java.
         */
        public void registerRoutes(Router router) {
            router.get("/api/articles/published", this::getPublishedArticles);
            router.get("/api/articles",           this::getAllArticles);
            router.get("/api/articles/{id}",      this::getArticleById);
            router.post("/api/articles",          this::createArticle);
            router.post("/api/articles/login",     this::checkLogin); // TODO: implement this handler
            router.put("/api/articles/{id}",      this::updateArticle);
            router.delete("/api/articles/{id}",   this::deleteArticle);
        }

        // ============================================================
        // HANDLER METHODS
        // Each method receives:
        //   exchange  = the HTTP request + response object
        //   pathVars  = path variables extracted from the URL (e.g. {"id": "42"})
        // ============================================================

        /**
         * GET /api/articles/published
         * Returns all published articles as a JSON array.
         * Used by: public home page
         */
        private void getPublishedArticles(HttpExchange exchange, Map<String, String> pathVars) {
            try {
                List<Article> articles = articleService.getAllPublishedArticles();
                String json = JsonUtil.articlesToJson(articles);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                Router.sendError(exchange, 500, "Failed to fetch articles: " + e.getMessage());
            }
        }

        /**
         * GET /api/articles
         * Returns ALL articles (including drafts) for admin dashboard.
         */
        private void getAllArticles(HttpExchange exchange, Map<String, String> pathVars) {
            try {
                List<Article> articles = articleService.getAllArticles();
                String json = JsonUtil.articlesToJson(articles);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                Router.sendError(exchange, 500, "Failed to fetch articles: " + e.getMessage());
            }
        }

        /**
         * GET /api/articles/{id}
         * Returns a single article by ID.
         *
         * pathVars.get("id") = the {id} value from the URL, as a String.
         * We parse it to a long before using it.
         */
        private void getArticleById(HttpExchange exchange, Map<String, String> pathVars) {
            try {
                long id = parseLongId(pathVars.get("id"));

                Optional<Article> article = articleService.getArticleById(id);

                if (article.isEmpty()) {
                    Router.sendError(exchange, 404, "Article not found with id: " + id);
                    return;
                }

                sendResponse(exchange, 200, JsonUtil.articleToJson(article.get()));
            } catch (NumberFormatException e) {
                Router.sendError(exchange, 400, "Invalid ID format");
            } catch (Exception e) {
                Router.sendError(exchange, 500, "Failed to fetch article: " + e.getMessage());
            }
        }

        /**
         * POST /api/articles
         * Creates a new article.
         *
         * Steps:
         * 1. Read the raw JSON body from the request InputStream
         * 2. Parse it into a Map with our JsonUtil
         * 3. Pass the Map to the service to validate and create the article
         * 4. Return 201 Created with the new article JSON
         */
        private void createArticle(HttpExchange exchange, Map<String, String> pathVars) {
            try {
                // Read the JSON request body as a String
                String body = readRequestBody(exchange);

                // DEBUG LOG: Print the incoming body to help diagnose POST issues
                System.out.println("[DEBUG] Incoming POST /api/articles body:\n" + body);

                // Parse JSON string → Map<String, String>
                Map<String, String> fields = JsonUtil.parseJsonBody(body);

                // Create the article (service validates fields)
                Article created = articleService.createArticle(fields);

                // Return 201 Created with the new article
                sendResponse(exchange, 201, JsonUtil.articleToJson(created));

            } catch (IllegalArgumentException e) {
                // Validation error (missing title, etc.)
                Router.sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                // Print full stacktrace to server console to aid debugging
                e.printStackTrace();
                Router.sendError(exchange, 500, "Failed to create article: " + e.getMessage());
            }
        }

        public void checkLogin(HttpExchange exchange, Map<String, String> pathVars) {
                try {
                    String body = readRequestBody(exchange);
                    Map<String, String> fields = JsonUtil.parseJsonBody(body);

                    String username = fields.get("username");
                    String password = fields.get("password");

                    User user = articleService.getAdmin();
                    if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                        sendResponse(exchange, 200, "{\"message\": \"Login successful\"}");
                    } else {
                        Router.sendError(exchange, 401, "Invalid username or password");
                    }
                } catch (IllegalArgumentException e) {
                    Router.sendError(exchange, 400, e.getMessage());
                }
                    catch (Exception e) {
                        Router.sendError(exchange, 500, "Failed to check login: " + e.getMessage());
                    }
        }

        /**
         * PUT /api/articles/{id}
         * Updates an existing article.
         */
        private void updateArticle(HttpExchange exchange, Map<String, String> pathVars) {
            try {
                long id = parseLongId(pathVars.get("id"));
                String body = readRequestBody(exchange);
                Map<String, String> fields = JsonUtil.parseJsonBody(body);

                Optional<Article> updated = articleService.updateArticle(id, fields);

                if (updated.isEmpty()) {
                    Router.sendError(exchange, 404, "Article not found with id: " + id);
                    return;
                }

                sendResponse(exchange, 200, JsonUtil.articleToJson(updated.get()));

            } catch (NumberFormatException e) {
                Router.sendError(exchange, 400, "Invalid ID format");
            } catch (IllegalArgumentException e) {
                Router.sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                Router.sendError(exchange, 500, "Failed to update article: " + e.getMessage());
            }
        }

        /**
         * DELETE /api/articles/{id}
         * Deletes an article by ID.
         * Returns 204 No Content on success (same as the Spring version).
         */
        private void deleteArticle(HttpExchange exchange, Map<String, String> pathVars) {
            try {
                long id = parseLongId(pathVars.get("id"));
                boolean deleted = articleService.deleteArticle(id);

                if (!deleted) {
                    Router.sendError(exchange, 404, "Article not found with id: " + id);
                    return;
                }

                // 204 No Content — success but nothing to return
                exchange.sendResponseHeaders(204, -1);
                exchange.close();

            } catch (NumberFormatException e) {
                Router.sendError(exchange, 400, "Invalid ID format");
            } catch (Exception e) {
                Router.sendError(exchange, 500, "Failed to delete article: " + e.getMessage());
            }
        }



        // ============================================================
        // PRIVATE UTILITY METHODS
        // ============================================================

        /**
         * READ THE HTTP REQUEST BODY as a String.
         *
         * In Spring, @RequestBody handled this automatically.
         * Here we read it from the InputStream manually.
         *
         * InputStream → byte array → String (UTF-8 decoded)
         */
        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        /**
         * SEND A JSON RESPONSE.
         *
         * In Spring, returning a ResponseEntity handled this.
         * Here we manually:
         * 1. Convert the JSON string to bytes (UTF-8)
         * 2. Set the Content-Length header (sendResponseHeaders requires it)
         * 3. Write the bytes to the response body
         * 4. Close the exchange
         */
        private void sendResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        /**
         * Parse a String ID from the URL to a long.
         * Throws NumberFormatException if it's not a valid number.
         */
        private long parseLongId(String idStr) {
            if (idStr == null || idStr.isBlank()) {
                throw new NumberFormatException("ID is missing");
            }
            return Long.parseLong(idStr);
        }
    }

