package com.blog.controller;

import com.blog.model.Article;
import com.blog.model.User;
import com.blog.server.Router;
import com.blog.service.AuthService;
import com.blog.service.ArticleService;
import com.blog.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;


    public class AuthController {
        private final AuthService authService = new AuthService();

        public void registerRoutes(Router router) {
            router.post("/api/auth/register", this::handleRegister);
            router.post("/api/auth/login", this::handleLogin);
        }

        public void handleRegister(HttpExchange exchange, Map<String, String> pathVars) {
           try {
                    String body = readRequestBody(exchange);
                    Map<String, String> fields = JsonUtil.parseJsonBody(body);

                    authService.register(fields);
    
                    // Return success response
                    sendResponse(exchange, 201, "{\"message\":\"User registered successfully\"}");
                }         catch (Exception e) {
                    e.printStackTrace();
                    try {
                        sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            
            
        }

        public void handleLogin(HttpExchange exchange, Map<String, String> pathVars) {
             try {
                    String body = readRequestBody(exchange);
                    Map<String, String> fields = JsonUtil.parseJsonBody(body);

                    if (fields.get("username") == null || fields.get("password") == null || fields.get("username").isBlank() || fields.get("password").isBlank()) {
                        String error = "{\"error\":\"username and password are required\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(400, error.length());
                        exchange.getResponseBody().write(error.getBytes());
                        exchange.getResponseBody().close();
                        return;
                    }

                    String token = authService.login(fields);
    
                    // Set token in HTTP-only cookie with 15-minute expiry
                    String cookieValue = String.format(
                        "token=%s; HttpOnly; Max-Age=900; Path=/",
                        token
                    );
                    exchange.getResponseHeaders().set("Set-Cookie", cookieValue);
                    
                    // Return success message
                    sendResponse(exchange, 200, "{\"message\":\"Login successful\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
        }





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



