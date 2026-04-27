package com.chatapp.models;

public class User {

    private String username;
    private String fullName;
    private boolean isOnline;

    public User(String username, String fullName) {
        this.username = username;
        this.fullName = fullName;
        this.isOnline = false;
    }

    public String getUsername()  { return username; }
    public String getFullName()  { return fullName; }
    public boolean isOnline()    { return isOnline; }
    public void setOnline(boolean online) { this.isOnline = online; }
}
