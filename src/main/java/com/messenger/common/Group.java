package com.messenger.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String adminId;
    private String description;
    private byte[] groupAvatar;
    private List<User> members;
    private LocalDateTime createdAt;

    public Group(String id, String name, String adminId, String description, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getGroupAvatar() {
        return groupAvatar;
    }

    public void setGroupAvatar(byte[] groupAvatar) {
        this.groupAvatar = groupAvatar;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", adminId='" + adminId + '\'' +
                ", membersCount=" + (members != null ? members.size() : 0) +
                '}';
    }
}
