package com.possystem.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Defaults match every existing workflow (IDE, run_*.bat, GitHub Actions CI) unchanged.
    // Override via environment variable when it's actually needed — e.g. docker-compose sets
    // DB_HOST=mysql, since "localhost" inside the api container would mean the container itself,
    // not the sibling mysql container.
    private static final String DB_HOST = env("DB_HOST", "localhost");
    private static final String DB_PORT = env("DB_PORT", "3306");
    private static final String DB_NAME = env("DB_NAME", "pos_system");

    private static final String DB_USER = env("DB_USER", "root");
    private static final String DB_PASSWORD = env("DB_PASSWORD", "Trf123");

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    // allowPublicKeyRetrieval=true is required because MySQL 8's default auth plugin
    // (caching_sha2_password) needs to exchange an RSA public key with the client, and the
    // driver refuses to do that automatically over a non-SSL connection unless this is set
    // explicitly (it's an opt-in, not a bug) — without it, login fails with "Public Key
    // Retrieval is not allowed" even with correct credentials. Safe here since this connects
    // to a local/trusted MySQL instance (localhost or the docker-compose sibling container),
    // not a remote one over an untrusted network.
    private static final String URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found on classpath. Add mysql-connector-j-*.jar to lib/", e);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }
}
