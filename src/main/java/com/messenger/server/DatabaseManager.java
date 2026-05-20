package com.messenger.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/messenger?user=postgres&password=postgres&sslmode=disable";

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
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN avatar_data BYTEA");
            } catch (SQLException e) {
                /* ignore */ }
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN last_seen TEXT");
            } catch (SQLException e) {
                /* ignore */ }

            // Re-create or Update Contacts table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS contacts (user_id TEXT, contact_id TEXT, PRIMARY KEY(user_id, contact_id))");
            try {
                stmt.execute("ALTER TABLE contacts ADD COLUMN status TEXT DEFAULT 'ACCEPTED'");
            } catch (SQLException e) { /* ignore */ }

            // Re-create or Update Messages table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS messages (id SERIAL PRIMARY KEY, message_uuid TEXT UNIQUE, sender_id TEXT, receiver_id TEXT, content TEXT, timestamp TEXT, status INT DEFAULT 0, file_name TEXT, file_data BYTEA)");
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
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN file_name TEXT");
            } catch (SQLException e) {
                /* ignore */ }
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN file_data BYTEA");
            } catch (SQLException e) {
                /* ignore */ }
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN parent_message_id TEXT");
            } catch (SQLException e) {
                /* ignore */ }
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN parent_message_content TEXT");
            } catch (SQLException e) { /* ignore */ }
            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN link_title TEXT");
                stmt.execute("ALTER TABLE messages ADD COLUMN link_description TEXT");
                stmt.execute("ALTER TABLE messages ADD COLUMN link_image_url TEXT");
            } catch (SQLException e) { /* ignore */ }

            stmt.execute("CREATE TABLE IF NOT EXISTS message_reactions (" +
                    "message_uuid TEXT NOT NULL, " +
                    "user_id TEXT NOT NULL, " +
                    "emoji TEXT NOT NULL, " +
                    "PRIMARY KEY (message_uuid, user_id), " +
                    "FOREIGN KEY (message_uuid) REFERENCES messages(message_uuid) ON DELETE CASCADE" +
                    ")");

            // Re-create or Update Groups, Members, and Receipts tables
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS groups (id TEXT PRIMARY KEY, name TEXT NOT NULL, admin_id TEXT NOT NULL, description TEXT, created_at TEXT NOT NULL, avatar_data BYTEA)");
            
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS group_members (group_id TEXT, user_id TEXT, PRIMARY KEY (group_id, user_id), FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS group_message_receipts (message_uuid TEXT, user_id TEXT, status INT DEFAULT 0, PRIMARY KEY (message_uuid, user_id), FOREIGN KEY (message_uuid) REFERENCES messages(message_uuid) ON DELETE CASCADE, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

            try {
                stmt.execute("ALTER TABLE messages ADD COLUMN group_id TEXT");
            } catch (SQLException e) {
                /* ignore if column exists */
            }

            // For testing setup
            stmt.execute(
                    "INSERT INTO users (id, username, password_hash, phone_number) VALUES ('admin_id', 'admin', 'admin', '0000') ON CONFLICT DO NOTHING");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
