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
        String sql = "SELECT id, username, phone_number, avatar_data, last_seen FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // Note: Simple raw password for demo, would hash in prod
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User u = new User(rs.getString("id"), rs.getString("username"), rs.getString("phone_number"));
                u.setAvatarData(rs.getBytes("avatar_data"));
                String ls = rs.getString("last_seen");
                if (ls != null) {
                    try { u.setLastSeen(java.time.LocalDateTime.parse(ls)); } catch (Exception ignored) {}
                }
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User register(String username, String password, String phoneNumber) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO users(id, username, password_hash, phone_number) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, phoneNumber);
            pstmt.executeUpdate();
            return new User(id, username, phoneNumber);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Handle uniqueness violations gracefully later
    }

    public boolean addContactByPhone(String userId, String phoneNumber) {
        String findIdSql = "SELECT id FROM users WHERE phone_number = ?";
        String targetId = null;
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(findIdSql)) {
            pstmt.setString(1, phoneNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                targetId = rs.getString("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (targetId == null)
            return false;

        String sql = "INSERT INTO contacts(user_id, contact_id, status) VALUES(?, ?, 'ACCEPTED') ON CONFLICT (user_id, contact_id) DO UPDATE SET status = 'ACCEPTED'";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, targetId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<User> getContactsForUser(String userId) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.phone_number, u.avatar_data, u.last_seen, c.status FROM users u JOIN contacts c ON u.id = c.contact_id WHERE c.user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User u = new User(rs.getString("id"), rs.getString("username"), rs.getString("phone_number"));
                    u.setAvatarData(rs.getBytes("avatar_data"));
                    u.setRelationshipStatus(rs.getString("status"));
                    String ls = rs.getString("last_seen");
                    if (ls != null) {
                        try { u.setLastSeen(java.time.LocalDateTime.parse(ls)); } catch (Exception ignored) {}
                    }
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateUserAvatar(String userId, byte[] avatarData) {
        String sql = "UPDATE users SET avatar_data = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, avatarData);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUserLastSeen(String userId, java.time.LocalDateTime lastSeen) {
        String sql = "UPDATE users SET last_seen = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lastSeen.toString());
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateContactStatus(String userId, String contactId, String status) {
        String sql = "INSERT INTO contacts(user_id, contact_id, status) VALUES(?, ?, ?) ON CONFLICT (user_id, contact_id) DO UPDATE SET status = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, contactId);
            pstmt.setString(3, status);
            pstmt.setString(4, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getContactStatus(String userId, String contactId) {
        String sql = "SELECT status FROM contacts WHERE user_id = ? AND contact_id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, contactId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
