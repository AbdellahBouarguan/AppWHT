package com.messenger.server;

import com.messenger.common.Group;
import com.messenger.common.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupDAO {

    public Group createGroup(String name, String adminId, List<String> memberIds, String description) {
        String groupId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String groupSql = "INSERT INTO groups(id, name, admin_id, description, created_at) VALUES(?, ?, ?, ?, ?)";
        String memberSql = "INSERT INTO group_members(group_id, user_id) VALUES(?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Enable transactional safety

            try (PreparedStatement groupPstmt = conn.prepareStatement(groupSql);
                 PreparedStatement memberPstmt = conn.prepareStatement(memberSql)) {

                // 1. Insert Group Metadata
                groupPstmt.setString(1, groupId);
                groupPstmt.setString(2, name);
                groupPstmt.setString(3, adminId);
                groupPstmt.setString(4, description);
                groupPstmt.setString(5, now.toString());
                groupPstmt.executeUpdate();

                // 2. Insert Memberships (including Admin)
                List<String> allMemberIds = new ArrayList<>();
                if (memberIds != null) {
                    allMemberIds.addAll(memberIds);
                }
                if (!allMemberIds.contains(adminId)) {
                    allMemberIds.add(adminId);
                }

                for (String memberId : allMemberIds) {
                    memberPstmt.setString(1, groupId);
                    memberPstmt.setString(2, memberId);
                    memberPstmt.addBatch();
                }
                memberPstmt.executeBatch();

                conn.commit(); // Save changes permanently

                Group newGroup = new Group(groupId, name, adminId, description, now);
                newGroup.setMembers(getGroupMembers(groupId));
                return newGroup;

            } catch (SQLException e) {
                conn.rollback(); // Undo everything on error
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Group> getGroupsForUser(String userId) {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.id, g.name, g.admin_id, g.description, g.created_at, g.avatar_data FROM groups g " +
                     "JOIN group_members gm ON g.id = gm.group_id WHERE gm.user_id = ? " +
                     "ORDER BY g.name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String adminId = rs.getString("admin_id");
                    String desc = rs.getString("description");
                    String caStr = rs.getString("created_at");
                    byte[] avatar = rs.getBytes("avatar_data");

                    LocalDateTime ca = LocalDateTime.now();
                    if (caStr != null) {
                        try {
                            ca = LocalDateTime.parse(caStr);
                        } catch (Exception ignored) {}
                    }

                    Group g = new Group(id, name, adminId, desc, ca);
                    g.setGroupAvatar(avatar);
                    g.setMembers(getGroupMembers(id));
                    groups.add(g);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public List<String> getGroupMemberIds(String groupId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("user_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<User> getGroupMembers(String groupId) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.phone_number, u.avatar_data, u.last_seen FROM users u " +
                     "JOIN group_members gm ON u.id = gm.user_id WHERE gm.group_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User u = new User(rs.getString("id"), rs.getString("username"), rs.getString("phone_number"));
                    u.setAvatarData(rs.getBytes("avatar_data"));
                    String ls = rs.getString("last_seen");
                    if (ls != null) {
                        try {
                            u.setLastSeen(LocalDateTime.parse(ls));
                        } catch (Exception ignored) {}
                    }
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean isGroup(String id) {
        String sql = "SELECT 1 FROM groups WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
