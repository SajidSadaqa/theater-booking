package org.example.util;

import org.example.exception.DbException;
import org.junit.Test;

public class DbValidatorTest {

    @Test(expected = DbException.class)
    public void testNullJdbcUrl() throws DbException {
        DbValidator.validateJdbcUrl(null);
    }

    @Test(expected = DbException.class)
    public void testEmptyJdbcUrl() throws DbException {
        DbValidator.validateJdbcUrl("");
    }

    @Test(expected = DbException.class)
    public void testInvalidJdbcUrlFormat() throws DbException {
        DbValidator.validateJdbcUrl("invalid-url");
    }

    @Test(expected = DbException.class)
    public void testJdbcUrlWithoutProtocol() throws DbException {
        DbValidator.validateJdbcUrl("mysql://localhost:3306/test");
    }

    @Test
    public void testValidH2JdbcUrl() throws DbException {
        DbValidator.validateJdbcUrl("jdbc:h2:mem:testdb");
        // Should not throw exception
    }

    @Test
    public void testValidMySqlJdbcUrl() throws DbException {
        DbValidator.validateJdbcUrl("jdbc:mysql://localhost:3306/theater");
        // Should not throw exception
    }

    @Test
    public void testValidPostgreSqlJdbcUrl() throws DbException {
        DbValidator.validateJdbcUrl("jdbc:postgresql://localhost:5432/theater");
        // Should not throw exception
    }

    @Test(expected = DbException.class)
    public void testNullUsername() throws DbException {
        DbValidator.validateCredentials(null, "password");
    }

    @Test(expected = DbException.class)
    public void testEmptyUsername() throws DbException {
        DbValidator.validateCredentials("", "password");
    }

    @Test(expected = DbException.class)
    public void testNullPassword() throws DbException {
        DbValidator.validateCredentials("username", null);
    }

    @Test
    public void testValidCredentials() throws DbException {
        DbValidator.validateCredentials("username", "password");

    }

    @Test
    public void testEmptyPasswordIsValid() throws DbException {
        DbValidator.validateCredentials("username", "");

    }
}
