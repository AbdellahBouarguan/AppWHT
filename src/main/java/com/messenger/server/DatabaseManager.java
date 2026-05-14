package com.messenger.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/messenger?user=postgres&password=postgres";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Re-create or Update Users table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, phone_number TEXT UNIQUE)");
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN phone_number TEXT");
            } catch (SQLException e) {
                /* ignore if column exists */ }

            // Re-create or Update Contacts table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS contacts (user_id TEXT, contact_id TEXT, PRIMARY KEY(user_id, contact_id))");

            // Re-create or Update Messages table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS messages (id SERIAL PRIMARY KEY, message_uuid TEXT UNIQUE, sender_id TEXT, receiver_id TEXT, content TEXT, timestamp TEXT, status INT DEFAULT 0)");
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN message_uuid TEXT UNIQUE");
            } catch (SQLException e) {
                /* ignore if column exists */ }
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN status INT DEFAULT 0");
            } catch (SQLException e) {
                /* ignore if column exists */ }
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN is_delivered INT DEFAULT 0");
            } catch (SQLException e) {
                /* ignore if column exists */ }

            // For testing setup
            stmt.execute(
                    "INSERT INTO users (id, username, password_hash, phone_number) VALUES ('admin_id', 'admin', 'admin', '0000') ON CONFLICT DO NOTHING");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
