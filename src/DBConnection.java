import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/Academia";
    private static final String USER = "sadik";
    private static final String PASS = "Mysql@123";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}