package com.blog.repository;

import com.blog.config.DatabaseManager;
import com.blog.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;




public class UserRepository {
      private DatabaseManager db = DatabaseManager.getInstance();

      public User findByUsername(String username) throws SQLException {
          String sql = "SELECT id, username, password, created_at FROM users WHERE username = ?";
          try (Connection conn = db.getConnection();
               PreparedStatement stmt = conn.prepareStatement(sql)) {
              stmt.setString(1, username);
              try (ResultSet rs = stmt.executeQuery()) {
                  if (rs.next()) {

                      java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                      if (createdAt != null) {

                      return new User(
                              rs.getString("username"),
                              rs.getString("password"),
                              rs.getLong("id"),
                              createdAt.toLocalDateTime()
                      );
                      } else {
                          return new User(
                              rs.getString("username"),
                              rs.getString("password"),
                              rs.getLong("id"),
                              null
                      );
                      }
                  } else {
                      return null; // Not found
                  }
              }
          }
      }


        public User findById(long id) throws SQLException {
            String sql = "SELECT id, username, password, created_at FROM users WHERE id = ?";
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                        if (createdAt != null) {
                            return new User(
                                    rs.getString("username"),
                                    rs.getString("password"),
                                    rs.getLong("id"),
                                    createdAt.toLocalDateTime()
                            );
                        } else {
                            return new User(
                                    rs.getString("username"),
                                    rs.getString("password"),
                                    rs.getLong("id"),
                                    null
                            );
                        }
                    } else {
                        return null; // Not found
                    }
                }
            }
        }

        public User save(User user) throws SQLException {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?) RETURNING id, created_at";
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        user.setId(rs.getLong("id"));
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                        if (createdAt != null) {
                            user.setCreatedAt(createdAt.toLocalDateTime());
                        }
                        return user;
                    } else {
                        throw new SQLException("Failed to insert user");
                    }
                }
            }
        }

}