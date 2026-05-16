package com.messenger.common;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String phoneNumber;
    private boolean isOnline;
    private byte[] avatarData;
    private java.time.LocalDateTime lastSeen;

    public User(String id, String username) {
        this.id = id;
        this.username = username;
        this.phoneNumber = "";
        this.isOnline = false;
    }

    public User(String id, String username, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.isOnline = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public byte[] getAvatarData() {
        return avatarData;
    }

    public void setAvatarData(byte[] avatarData) {
        this.avatarData = avatarData;
    }

    public java.time.LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(java.time.LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public String toString() {
        return username + (phoneNumber != null && !phoneNumber.isEmpty() ? " (" + phoneNumber + ")" : "")
                + (isOnline ? " (Online)" : " (Offline)");
    }
}
