package com.messenger.server;

import com.messenger.common.Message;
import com.messenger.common.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MessageDAO {
    public void saveMessage(Message msg) {
        if (msg.getSender() == null || msg.getReceiver() == null)
            return;

        String sql = "INSERT INTO messages(sender_id, receiver_id, content, timestamp, is_delivered) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msg.getSender().getId());
            pstmt.setString(2, msg.getReceiver().getId());
            pstmt.setString(3, msg.getContent());
            pstmt.setString(4, msg.getTimestamp().toString());
            pstmt.setInt(5, msg.isDelivered() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateMessageDelivered(int messageId) {
        String sql = "UPDATE messages SET is_delivered = 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<Message> getUndeliveredMessages(String receiverId) {
        java.util.List<Message> list = new java.util.ArrayList<>();
        String sql = "SELECT m.id, m.sender_id, u.username as sender_name, m.content, m.timestamp FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.receiver_id = ? AND m.is_delivered = 0";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getString("sender_id"), rs.getString("sender_name"));
                    User receiver = new User(receiverId, "");
                    Message msg = new Message(sender, receiver, rs.getString("content"));
                    msg.setDelivered(false);
                    // store messageId inside a dummy wrapper or set logic. We'll simply update all
                    // by receiver ID instead in server.
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void markAllAsDelivered(String receiverId) {
        String sql = "UPDATE messages SET is_delivered = 1 WHERE receiver_id = ? AND is_delivered = 0";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<Message> getChatHistory(String userId1, String userId2) {
        java.util.List<Message> list = new java.util.ArrayList<>();
        String sql = "SELECT m.id, m.sender_id, u1.username as sender_name, m.receiver_id, u2.username as receiver_name, m.content, m.timestamp FROM messages m "
                +
                "JOIN users u1 ON m.sender_id = u1.id " +
                "JOIN users u2 ON m.receiver_id = u2.id " +
                "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                "ORDER BY m.id ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId1);
            pstmt.setString(2, userId2);
            pstmt.setString(3, userId2);
            pstmt.setString(4, userId1);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getString("sender_id"), rs.getString("sender_name"));
                    User receiver = new User(rs.getString("receiver_id"), rs.getString("receiver_name"));
                    Message msg = new Message(sender, receiver, rs.getString("content"));
                    try {
                        msg.setTimestamp(java.time.LocalDateTime.parse(rs.getString("timestamp")));
                    } catch (Exception e) {
                    }
                    msg.setDelivered(true);
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
