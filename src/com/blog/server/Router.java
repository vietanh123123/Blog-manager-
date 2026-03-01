    package com.blog.server;

    import com.sun.net.httpserver.HttpExchange;
    import com.sun.net.httpserver.HttpHandler;

    import java.io.IOException;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.function.BiConsumer;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    /**
     * HTTP ROUTER
     *
     * WHAT DID SPRING DO?
     * In the Spring version, you just wrote:
     *   @GetMapping("/api/articles/{id}")
     *   public ResponseEntity<Article> getById(@PathVariable Long id) { ... }
     *
     * Spring scanned all your @Controller classes, found every @GetMapping,
     * @PostMapping etc., and built a routing table automatically.
     *
     * WHAT WE DO NOW:
     * We build the routing table ourselves. This Router:
     *   1. Stores routes as: (HTTP method + URL pattern) → handler function
     *   2. When a request comes in, finds the matching route
     *   3. Extracts path variables (like {id}) from the URL
     *   4. Calls the appropriate handler function
     *
     * ROUTES WE REGISTER:
     *   GET    /api/articles/published  → get published articles
     *   GET    /api/articles            → get all articles (admin)
     *   GET    /api/articles/{id}       → get single article
     *   POST   /api/articles            → create article
     *   PUT    /api/articles/{id}       → update article
     *   DELETE /api/articles/{id}       → delete article
     *
     * BiConsumer<HttpExchange, Map<String, String>>:
     *   = a function that takes (HttpExchange, pathVariables) and returns nothing
     *   HttpExchange  = the incoming request + outgoing response object
     *   pathVariables = extracted {id} values from the URL as a Map
     */
    public class Router implements HttpHandler {

        /**
         * Represents one registered route.
         * e.g.: method="GET", pattern="/api/articles/{id}", handler=getByIdFunction
         */
        private static class Route {
            final String method;
            final Pattern pattern;      // compiled regex for URL matching
            final String[] groupNames;  // names of path variables (e.g. "id")
            final BiConsumer<HttpExchange, Map<String, String>> handler;

            Route(String method, String urlPattern,
                  BiConsumer<HttpExchange, Map<String, String>> handler) {
                this.method = method;
                this.handler = handler;

                // Convert "/api/articles/{id}" → regex "/api/articles/([^/]+)"
                // and remember the variable names in order
                java.util.List<String> names = new java.util.ArrayList<>();
                // Find all {variableName} placeholders
                java.util.regex.Matcher m =
                        java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(urlPattern);

                StringBuffer regexBuffer = new StringBuffer();
                while (m.find()) {
                    names.add(m.group(1));          // save the variable name (e.g. "id")
                    m.appendReplacement(regexBuffer, "([^/]+)"); // replace with capture group
                }
                m.appendTail(regexBuffer);

                this.groupNames = names.toArray(new String[0]);
                this.pattern = Pattern.compile("^" + regexBuffer + "$");
            }
        }

        private final java.util.List<Route> routes = new java.util.ArrayList<>();

        // ============================================================
        // ROUTE REGISTRATION METHODS
        // ============================================================
        // These mimic Spring's @GetMapping, @PostMapping, etc.

        public void get(String path, BiConsumer<HttpExchange, Map<String, String>> handler) {
            routes.add(new Route("GET", path, handler));
        }

        public void post(String path, BiConsumer<HttpExchange, Map<String, String>> handler) {
            routes.add(new Route("POST", path, handler));
        }

        public void put(String path, BiConsumer<HttpExchange, Map<String, String>> handler) {
            routes.add(new Route("PUT", path, handler));
        }

        public void delete(String path, BiConsumer<HttpExchange, Map<String, String>> handler) {
            routes.add(new Route("DELETE", path, handler));
        }

        // ============================================================
        // REQUEST HANDLING
        // ============================================================

        /**
         * This is called by Java's HttpServer for EVERY incoming HTTP request.
         *
         * Steps:
         * 1. Add CORS headers (allow frontend to call us)
         * 2. Handle OPTIONS preflight request (browser's CORS check)
         * 3. Find the matching route
         * 4. Extract path variables
         * 5. Call the handler
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 1. Add CORS headers to every response
            addCorsHeaders(exchange);

            String method = exchange.getRequestMethod();

            // 2. Handle OPTIONS preflight (browser sends this before actual request)
            // Browser: "Hey server, can I make a POST from localhost:5500 to you?"
            // Server:  "Yes, here are the allowed methods/headers"
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1); // 204 = No Content
                exchange.close();
                return;
            }

            // 3. Get the URL path (e.g. "/api/articles/42")
            String path = exchange.getRequestURI().getPath();

            // 4. Find a matching route
            for (Route route : routes) {
                if (!route.method.equals(method)) continue; // wrong HTTP method

                Matcher matcher = route.pattern.matcher(path);
                if (!matcher.matches()) continue; // URL doesn't match

                // 5. Extract path variables (e.g. {id} = "42")
                Map<String, String> pathVars = new HashMap<>();
                for (int i = 0; i < route.groupNames.length; i++) {
                    pathVars.put(route.groupNames[i], matcher.group(i + 1));
                }

                // 6. Call the handler
                try {
                    route.handler.accept(exchange, pathVars);
                } catch (Exception e) {
                    System.err.println("Handler error: " + e.getMessage());
                    e.printStackTrace();
                    sendError(exchange, 500, "Internal server error");
                }
                return;
            }

            // No route matched → 404 Not Found
            sendError(exchange, 404, "No route found for " + method + " " + path);
        }

        // ============================================================
        // UTILITY METHODS
        // ============================================================

        /**
         * CORS HEADERS
         * Same purpose as our CorsConfig.java in the Spring version.
         * Tells the browser which origins, methods, and headers are allowed.
         */
        private void addCorsHeaders(HttpExchange exchange) {
            var headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            headers.set("Content-Type", "application/json");
        }

        /**
         * Send a JSON error response.
         */
        public static void sendError(HttpExchange exchange, int statusCode, String message) {
            try {
                String body = "{\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
                byte[] bytes = body.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            } catch (IOException e) {
                System.err.println("Failed to send error response: " + e.getMessage());
            }
        }
    }