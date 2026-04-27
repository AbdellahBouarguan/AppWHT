package com.messenger.server;

import com.messenger.common.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MessageDAO {
    public void saveMessage(Message msg) {
        if (msg.getSender() == null || msg.getReceiver() == null)
            return;

        String sql = "INSERT INTO messages(sender_id, receiver_id, content, timestamp) VALUES(?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msg.getSender().getId());
            pstmt.setString(2, msg.getReceiver().getId());
            pstmt.setString(3, msg.getContent());
            pstmt.setString(4, msg.getTimestamp().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
