package db.borealis;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BorealisDriverTest {
    private static final String profile = System.getenv("AWS_PROFILE");
    private static final String region = System.getenv("AWS_DEFAULT_REGION");

    private static final String URL =
        "jdbc:borealis:%s:%s:%s".formatted(
            profile != null ? profile : "dev",
            region != null ? region : "us-west-2",
            "borealis-integration-test" // use same value as test_iac.py
        );

    @Test
    public void testCreateTableInsertAndSelect() throws SQLException {
        System.out.println("Test started");
        DriverManager.registerDriver(new BorealisDriver());

        Connection conn = DriverManager.getConnection(URL);

        try (Statement stmt = conn.createStatement()) {
            // Create a table
            stmt.executeUpdate("DROP TABLE IF EXISTS users;");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT)");

            // Insert data
            stmt.executeUpdate("INSERT INTO users (id, name) VALUES (1, 'John Doe')");
            stmt.executeUpdate("INSERT INTO users (id, name) VALUES (2, 'Jane Smith')");

            // Select data
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");

            int rowCount = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                System.out.println("ID: " + id + ", Name: " + name);
                rowCount++;
            }

            assertEquals(2, rowCount, "Expected 2 rows in the result set");
            System.out.println("Test passed");
        } finally {
            conn.close();
        }
    }

    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }
}