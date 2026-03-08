package com.blog.service;

import com.blog.model.Article;
import com.blog.model.User;
import com.blog.repository.UserRepository;
import com.blog.repository.ArticleRepository;
import com.blog.util.PasswordUtil;
import com.blog.util.JwtUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository = new UserRepository();

    public User register(Map<String, String> fields) throws SQLException {
        String username = fields.get("username");
        String password = fields.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }

        String hashedPassword = PasswordUtil.hash(password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public String login(Map<String, String> fields) throws SQLException {
        String username = fields.get("username");
        String password = fields.get("password");
        
        
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        
        return JwtUtil.generateToken(user.getId(), user.getUsername());
    }
}