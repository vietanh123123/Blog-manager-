package com.blog.model;
import java.time.LocalDateTime;

public class User {

    private long userId;
    private String username;
    private String password;
    private LocalDateTime createdAt;

    public User(String name, String password, long userId, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = name;
        this.password = password;
        this.createdAt = createdAt;
    }
    public User() {} 
    public User (String name, String password) {
        this.username = name;
        this.password = password;
    }   

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public long getUserId() {
        return this.userId;
    }
    public long getId() {
        return this.userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {this.password = password;}
    public void setId (long userId) {
        this.userId = userId;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}