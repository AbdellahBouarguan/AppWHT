package com.messenger.server;

import com.messenger.common.Message;
import com.messenger.common.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MessageDAO {
    public void saveMessage(Message msg) {
        if (msg.getSender() == null || (msg.getReceiver() == null && msg.getGroupId() == null))
            return;

        String sql = "INSERT INTO messages(message_uuid, sender_id, receiver_id, content, timestamp, status, file_name, file_data, parent_message_id, parent_message_content, link_title, link_description, link_image_url, group_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msg.getId());
            pstmt.setString(2, msg.getSender().getId());
            pstmt.setString(3, msg.getReceiver() != null ? msg.getReceiver().getId() : null);
            pstmt.setString(4, msg.getContent());
            pstmt.setString(5, msg.getTimestamp().toString());
            pstmt.setInt(6, msg.getStatus().ordinal());
            pstmt.setString(7, msg.getFileName());
            pstmt.setBytes(8, msg.getFileData());
            pstmt.setString(9, msg.getParentMessageId());
            pstmt.setString(10, msg.getParentMessageContent());
            pstmt.setString(11, msg.getLinkTitle());
            pstmt.setString(12, msg.getLinkDescription());
            pstmt.setString(13, msg.getLinkImageUrl());
            pstmt.setString(14, msg.getGroupId());
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
        String sql = "SELECT m.message_uuid, m.sender_id, u.username as sender_name, m.content, m.timestamp, m.status, m.file_name, m.file_data, m.parent_message_id, m.parent_message_content, m.link_title, m.link_description, m.link_image_url, m.group_id " +
                     "FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.receiver_id = ? AND m.status < 2 " +
                     "UNION ALL " +
                     "SELECT m.message_uuid, m.sender_id, u.username as sender_name, m.content, m.timestamp, r.status as status, m.file_name, m.file_data, m.parent_message_id, m.parent_message_content, m.link_title, m.link_description, m.link_image_url, m.group_id " +
                     "FROM messages m JOIN users u ON m.sender_id = u.id JOIN group_message_receipts r ON m.message_uuid = r.message_uuid WHERE r.user_id = ? AND r.status < 2 " +
                     "ORDER BY timestamp ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, receiverId);
            pstmt.setString(2, receiverId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getString("sender_id"), rs.getString("sender_name"));
                    String groupId = rs.getString("group_id");
                    User receiver = groupId != null ? null : new User(receiverId, "");
                    Message msg = new Message(sender, receiver, rs.getString("content"));
                    if (groupId != null) {
                        msg.setGroupId(groupId);
                    }
                    String id = rs.getString("message_uuid");
                    if (id != null) msg.setId(id);
                    try {
                        msg.setTimestamp(java.time.LocalDateTime.parse(rs.getString("timestamp")));
                    } catch (Exception e) {}
                    msg.setStatus(Message.MessageStatus.values()[rs.getInt("status")]);
                     msg.setFileName(rs.getString("file_name"));
                    msg.setFileData(rs.getBytes("file_data"));
                    msg.setParentMessageId(rs.getString("parent_message_id"));
                    msg.setParentMessageContent(rs.getString("parent_message_content"));
                    msg.setLinkTitle(rs.getString("link_title"));
                    msg.setLinkDescription(rs.getString("link_description"));
                    msg.setLinkImageUrl(rs.getString("link_image_url"));
                    msg.setReactions(fetchReactions(id));
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
        String groupSql = "UPDATE group_message_receipts SET status = 2 WHERE user_id = ? AND status < 2";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt1 = conn.prepareStatement(sql);
             PreparedStatement pstmt2 = conn.prepareStatement(groupSql)) {
            pstmt1.setString(1, receiverId);
            pstmt1.executeUpdate();

            pstmt2.setString(1, receiverId);
            pstmt2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markAllAsRead(String senderId, String receiverId) {
        String sql = "UPDATE messages SET status = 3 WHERE sender_id = ? AND receiver_id = ? AND status < 3";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, senderId);
            pstmt.setString(2, receiverId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<Message> getChatHistory(String userId1, String userId2) {
        java.util.List<Message> list = new java.util.ArrayList<>();
        String sql = "SELECT m.message_uuid, m.sender_id, u1.username as sender_name, m.receiver_id, u2.username as receiver_name, m.content, m.timestamp, m.status, m.file_name, m.file_data, m.parent_message_id, m.parent_message_content, m.link_title, m.link_description, m.link_image_url FROM messages m "
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
                    msg.setParentMessageId(rs.getString("parent_message_id"));
                    msg.setParentMessageContent(rs.getString("parent_message_content"));
                    msg.setLinkTitle(rs.getString("link_title"));
                    msg.setLinkDescription(rs.getString("link_description"));
                    msg.setLinkImageUrl(rs.getString("link_image_url"));
                    msg.setReactions(fetchReactions(id));
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void deleteMessage(String uuid) {
        String sql = "UPDATE messages SET content = 'This message was deleted', file_name = NULL, file_data = NULL WHERE message_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addReaction(String msgUuid, String userId, String emoji) {
        String sql = "INSERT INTO message_reactions (message_uuid, user_id, emoji) VALUES (?, ?, ?) " +
                     "ON CONFLICT (message_uuid, user_id) DO UPDATE SET emoji = EXCLUDED.emoji";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msgUuid);
            pstmt.setString(2, userId);
            pstmt.setString(3, emoji);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private java.util.Map<String, String> fetchReactions(String msgUuid) {
        java.util.Map<String, String> reactions = new java.util.HashMap<>();
        String sql = "SELECT user_id, emoji FROM message_reactions WHERE message_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msgUuid);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reactions.put(rs.getString("user_id"), rs.getString("emoji"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reactions;
    }
    public void saveGroupMessageReceipt(String messageUuid, String userId, int status) {
        String sql = "INSERT INTO group_message_receipts(message_uuid, user_id, status) VALUES(?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, messageUuid);
            pstmt.setString(2, userId);
            pstmt.setInt(3, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateGroupMessageReceipt(String messageUuid, String userId, int status) {
        String sql = "UPDATE group_message_receipts SET status = ? WHERE message_uuid = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, status);
            pstmt.setString(2, messageUuid);
            pstmt.setString(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.List<Message> getGroupChatHistory(String groupId) {
        java.util.List<Message> list = new java.util.ArrayList<>();
        String sql = "SELECT m.message_uuid, m.sender_id, u.username as sender_name, m.content, m.timestamp, m.status, m.file_name, m.file_data, m.parent_message_id, m.parent_message_content, m.link_title, m.link_description, m.link_image_url FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.group_id = ? ORDER BY m.id ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User sender = new User(rs.getString("sender_id"), rs.getString("sender_name"));
                    Message msg = new Message(sender, (User) null, rs.getString("content"));
                    msg.setGroupId(groupId);
                    String id = rs.getString("message_uuid");
                    if (id != null) msg.setId(id);
                    try {
                        msg.setTimestamp(java.time.LocalDateTime.parse(rs.getString("timestamp")));
                    } catch (Exception e) {}
                    msg.setStatus(Message.MessageStatus.values()[rs.getInt("status")]);
                    msg.setFileName(rs.getString("file_name"));
                    msg.setFileData(rs.getBytes("file_data"));
                    msg.setParentMessageId(rs.getString("parent_message_id"));
                    msg.setParentMessageContent(rs.getString("parent_message_content"));
                    msg.setLinkTitle(rs.getString("link_title"));
                    msg.setLinkDescription(rs.getString("link_description"));
                    msg.setLinkImageUrl(rs.getString("link_image_url"));
                    msg.setReactions(fetchReactions(id));
                    list.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
