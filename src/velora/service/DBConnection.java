package velora.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single place that knows how to reach MySQL.
 * Credentials come from db.properties in the project root (kept out of Git),
 * or from the environment variables VELORA_DB_URL / VELORA_DB_USER / VELORA_DB_PASSWORD.
 */
public class DBConnection {
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException ignored) {
            // No file: fall back to environment variables and defaults
        }
        URL = pick(props, "db.url", "VELORA_DB_URL", "jdbc:mysql://localhost:3306/velora_db");
        USER = pick(props, "db.user", "VELORA_DB_USER", "root");
        PASSWORD = pick(props, "db.password", "VELORA_DB_PASSWORD", "");
    }

    private static String pick(Properties props, String key, String env, String fallback) {
        String fromEnv = System.getenv(env);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        return props.getProperty(key, fallback);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static String describe() {
        return USER + " @ " + URL;
    }
}
