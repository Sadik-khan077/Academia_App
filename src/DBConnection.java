import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/your_database";
    private static final String USER = "user";
    private static final String PASS = "Your_pass";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}