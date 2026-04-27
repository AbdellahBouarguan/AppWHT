package com.messenger.server;

import com.messenger.common.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDAO {

    public User authenticate(String username, String password) {
        String sql = "SELECT id, username, role FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // Note: Simple raw password for demo, would hash in prod
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("id"), rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User register(String username, String password) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO users(id, username, password_hash, role, isOnline) VALUES(?, ?, ?, 'USER', 0)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.executeUpdate();
            return new User(id, username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Handle uniqueness violations gracefully later
    }

    public void updateOnlineStatus(String id, boolean isOnline) {
        String sql = "UPDATE users SET isOnline = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isOnline);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> getOnlineUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username FROM users WHERE isOnline = 1";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                User u = new User(rs.getString("id"), rs.getString("username"));
                u.setOnline(true);
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
