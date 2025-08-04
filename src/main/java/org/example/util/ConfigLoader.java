package org.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        properties = new Properties();

        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("Config file not found: " + CONFIG_FILE + ". Using environment variables.");
                loadFromEnvironment();
                return;
            }

            properties.load(input);
            System.out.println("Configuration loaded from " + CONFIG_FILE);

        } catch (IOException e) {
            System.err.println("Error loading config file: " + e.getMessage());
            loadFromEnvironment();
        }
    }

    private static void loadFromEnvironment() {
        // Fallback to environment variables with PostgreSQL defaults
        properties.setProperty("db.url", System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/theaterdb"));
        properties.setProperty("db.username", System.getenv().getOrDefault("DB_USERNAME", "theater_user"));
        properties.setProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", "theater_password"));
        properties.setProperty("db.driver", System.getenv().getOrDefault("DB_DRIVER", "org.postgresql.Driver"));

        // Connection pool defaults
        properties.setProperty("db.pool.maximumPoolSize", System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10"));
        properties.setProperty("db.pool.minimumIdle", System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2"));
        properties.setProperty("db.pool.connectionTimeout", System.getenv().getOrDefault("DB_POOL_CONNECTION_TIMEOUT", "30000"));
        properties.setProperty("db.pool.idleTimeout", System.getenv().getOrDefault("DB_POOL_IDLE_TIMEOUT", "600000"));
        properties.setProperty("db.pool.maxLifetime", System.getenv().getOrDefault("DB_POOL_MAX_LIFETIME", "1800000"));
        properties.setProperty("db.pool.leakDetectionThreshold", System.getenv().getOrDefault("DB_POOL_LEAK_DETECTION", "60000"));

        System.out.println("Using environment variables for configuration");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for property '" + key + "': " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    public static long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("Invalid long value for property '" + key + "': " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get all properties (useful for debugging)
     */
    public static Properties getAllProperties() {
        return new Properties(properties);
    }

    /**
     * Check if a property exists
     */
    public static boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Print all loaded properties (without sensitive values)
     */
    public static void printConfiguration() {
        System.out.println("=== Current Configuration ===");
        properties.forEach((key, value) -> {
            String keyStr = key.toString();
            String valueStr = value.toString();

            // Hide sensitive information
            if (keyStr.toLowerCase().contains("password") || keyStr.toLowerCase().contains("secret")) {
                valueStr = "****";
            }

            System.out.println(keyStr + " = " + valueStr);
        });
        System.out.println("==============================");
    }
}