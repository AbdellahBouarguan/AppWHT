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

        String sql = "INSERT INTO messages(message_uuid, sender_id, receiver_id, content, timestamp, status, file_name, file_data) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msg.getId());
            pstmt.setString(2, msg.getSender().getId());
            pstmt.setString(3, msg.getReceiver().getId());
            pstmt.setString(4, msg.getContent());
            pstmt.setString(5, msg.getTimestamp().toString());
            pstmt.setInt(6, msg.getStatus().ordinal());
            pstmt.setString(7, msg.getFileName());
            pstmt.setBytes(8, msg.getFileData());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateMessageStatus(String messageId, Message.MessageStatus status) {
        String sql = "UPDATE messages SET status = ? WHERE message_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, status.ordinal());
            pstmt.setString(2, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<Message> getUndeliveredMessages(String receiverId) {
        java.util.List<Message> list = new java.util.ArrayList<>();
        String sql = "SELECT m.message_uuid, m.sender_id, u.username as sender_name, m.content, m.timestamp, m.status, m.file_name, m.file_data FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.receiver_id = ? AND m.status < 2"; // < DELIVERED_TO_CLIENT
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getString("sender_id"), rs.getString("sender_name"));
                    User receiver = new User(receiverId, "");
                    Message msg = new Message(sender, receiver, rs.getString("content"));
                    String id = rs.getString("message_uuid");
                    if (id != null) msg.setId(id);
                    try {
                        msg.setTimestamp(java.time.LocalDateTime.parse(rs.getString("timestamp")));
                    } catch (Exception e) {}
                    msg.setStatus(Message.MessageStatus.values()[rs.getInt("status")]);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileData(rs.getBytes("file_data"));
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void markAllAsDelivered(String receiverId) {
        String sql = "UPDATE messages SET status = 2 WHERE receiver_id = ? AND status < 2";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<Message> getChatHistory(String userId1, String userId2) {
        java.util.List<Message> list = new java.util.ArrayList<>();
        String sql = "SELECT m.message_uuid, m.sender_id, u1.username as sender_name, m.receiver_id, u2.username as receiver_name, m.content, m.timestamp, m.status, m.file_name, m.file_data FROM messages m "
                + "JOIN users u1 ON m.sender_id = u1.id " +
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
                    String id = rs.getString("message_uuid");
                    if (id != null) msg.setId(id);
                    try {
                        msg.setTimestamp(java.time.LocalDateTime.parse(rs.getString("timestamp")));
                    } catch (Exception e) {}
                    msg.setStatus(Message.MessageStatus.values()[rs.getInt("status")]);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileData(rs.getBytes("file_data"));
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
