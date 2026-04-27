package com.messenger.common;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private boolean isOnline;

    public User(String id, String username) {
        this.id = id;
        this.username = username;
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

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
    
    @Override
    public String toString() {
        return username + (isOnline ? " (Online)" : " (Offline)");
    }
}
