package org.example.util;

import org.example.exception.DbException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void initialize() throws DbException {
        String url = ConfigLoader.getProperty("db.url");
        String username = ConfigLoader.getProperty("db.username");
        String password = ConfigLoader.getProperty("db.password");
        String driver = ConfigLoader.getProperty("db.driver");

        // Validate credentials
        DbValidator.validateCredentials(url, username, password);

        // Test connection
        DbValidator.testConnection(url, username, password);

        // Setup connection pool
        setupConnectionPool(url, username, password, driver);

        // Create tables
        createTables();

        System.out.println("PostgreSQL database initialized successfully.");
    }

    private void setupConnectionPool(String url, String username, String password, String driver) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);

        // Pool settings
        config.setMaximumPoolSize(ConfigLoader.getIntProperty("db.pool.maximumPoolSize", 10));
        config.setMinimumIdle(ConfigLoader.getIntProperty("db.pool.minimumIdle", 2));
        config.setConnectionTimeout(ConfigLoader.getIntProperty("db.pool.connectionTimeout", 30000));
        config.setIdleTimeout(ConfigLoader.getLongProperty("db.pool.idleTimeout", 600000));
        config.setMaxLifetime(ConfigLoader.getLongProperty("db.pool.maxLifetime", 1800000));

        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        // Connection validation
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(ConfigLoader.getLongProperty("db.pool.leakDetectionThreshold", 60000));

        dataSource = new HikariDataSource(config);
    }

    private void createTables() throws DbException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create theaters table with PostgreSQL syntax
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theaters (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create sections table with PostgreSQL syntax
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sections (
                    id BIGSERIAL PRIMARY KEY,
                    theater_id BIGINT NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (theater_id) REFERENCES theaters(id) ON DELETE CASCADE,
                    UNIQUE (theater_id, name)
                )
            """);

            // Create rows table with PostgreSQL syntax
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rows (
                    id BIGSERIAL PRIMARY KEY,
                    section_id BIGINT NOT NULL,
                    number INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
                    UNIQUE (section_id, number)
                )
            """);

            // Create seats table with PostgreSQL syntax
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS seats (
                    id BIGSERIAL PRIMARY KEY,
                    row_id BIGINT NOT NULL,
                    number INTEGER NOT NULL,
                    status VARCHAR(20) DEFAULT 'AVAILABLE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (row_id) REFERENCES rows(id) ON DELETE CASCADE,
                    UNIQUE (row_id, number)
                )
            """);

            // Create indexes for better performance
            createIndexes(stmt);

            // Create trigger for updated_at column
            createUpdatedAtTrigger(stmt);

            System.out.println("PostgreSQL database tables and indexes created/verified successfully.");

        } catch (SQLException e) {
            throw new DbException("Failed to create database tables: " + e.getMessage(), e);
        }
    }

    private void createIndexes(Statement stmt) throws SQLException {
        try {
            // Index on theater name for faster lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_theaters_name ON theaters(name)");

            // Index on sections for theater lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sections_theater_id ON sections(theater_id)");

            // Index on rows for section lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rows_section_id ON rows(section_id)");

            // Index on seats for row lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_seats_row_id ON seats(row_id)");

            // Index on seat status for availability queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_seats_status ON seats(status)");

            // Composite index for seat booking queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_seats_row_number ON seats(row_id, number)");

            System.out.println("Database indexes created successfully.");
        } catch (SQLException e) {
            System.err.println("Warning: Could not create some indexes: " + e.getMessage());
            // Don't throw exception for index creation failures
        }
    }

    private void createUpdatedAtTrigger(Statement stmt) throws SQLException {
        try {
            // Create function to update the updated_at column
            stmt.execute("""
                CREATE OR REPLACE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = CURRENT_TIMESTAMP;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
            """);

            // Drop existing trigger if it exists
            stmt.execute("DROP TRIGGER IF EXISTS update_seats_updated_at ON seats");

            // Create trigger on seats table
            stmt.execute("""
                CREATE TRIGGER update_seats_updated_at
                    BEFORE UPDATE ON seats
                    FOR EACH ROW
                    EXECUTE FUNCTION update_updated_at_column()
            """);

            System.out.println("Database triggers created successfully.");
        } catch (SQLException e) {
            System.err.println("Warning: Could not create triggers: " + e.getMessage());
            // Don't throw exception for trigger creation failures
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("PostgreSQL database connection pool closed.");
        }
    }
}