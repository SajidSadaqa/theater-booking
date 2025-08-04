package org.example.util;

import org.example.exception.DbException;

import java.sql.DriverManager;
import java.util.regex.Pattern;

public class DbValidator {

    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("^jdbc:[a-zA-Z0-9_]+://[a-zA-Z0-9._-]+(?::[0-9]+)?(?:/[^?]*)?(?:\\?.*)?$|^jdbc:[a-zA-Z0-9_]+:[^:]+$");

    public static void validateCredentials(String url, String username, String password) throws DbException {
        validateJdbcUrl(url);
        validateCredentials(username, password);
    }

    public static void validateJdbcUrl(String url) throws DbException {
        if (url == null || url.trim().isEmpty()) {
            throw new DbException("JDBC URL cannot be null or empty");
        }

        if (!url.startsWith("jdbc:")) {
            throw new DbException("Invalid JDBC URL format: must start with 'jdbc:'");
        }

        if (!JDBC_URL_PATTERN.matcher(url).matches()) {
            throw new DbException("Invalid JDBC URL format: " + url);
        }
    }

    public static void validateCredentials(String username, String password) throws DbException {
        if (username == null || username.trim().isEmpty()) {
            throw new DbException("Database username cannot be null or empty");
        }

        if (password == null) {
            throw new DbException("Database password cannot be null");
        }
    }


    public static void testConnection(String url, String username, String password) throws DbException {
        try {
            DriverManager.getConnection(url, username, password).close();
        } catch (Exception e) {
            throw new DbException("Failed to connect to database: " + e.getMessage(), e);
        }
    }
}