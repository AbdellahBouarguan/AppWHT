package com.messenger.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:messenger.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT UNIQUE, password_hash TEXT, role TEXT, isOnline BOOLEAN)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender_id TEXT, receiver_id TEXT, content TEXT, timestamp TEXT)");
            // Create a default admin user if not exists
            stmt.execute(
                    "INSERT OR IGNORE INTO users (id, username, password_hash, role, isOnline) VALUES ('admin_id', 'admin', 'admin', 'ADMIN', 0)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
