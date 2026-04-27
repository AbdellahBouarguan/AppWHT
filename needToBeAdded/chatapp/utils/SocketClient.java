package com.chatapp.utils;

public class SocketClient {

    public static boolean connect(String username, String password) {
        return username.equals("admin") && password.equals("1234");
    }

    public static boolean register(String fullName, String username, String password) {
        return true;
    }
}
