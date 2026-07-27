package com.possystem.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "pos_system";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Trf123";

    private static final String URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&serverTimezone=UTC";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found on classpath. Add mysql-connector-j-*.jar to lib/", e);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }
}
