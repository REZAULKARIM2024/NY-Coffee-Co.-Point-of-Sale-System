package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;

/**
 * One-off seeding utility: reads a plain .sql file of INSERT statements and executes them
 * against the app's own configured database via {@link DBConnection#getConnection()} — the
 * same connection path the running app already uses, so no credentials are entered anywhere
 * by hand.
 *
 * Usage: java -cp target/classes;lib/mysql-connector-j-*.jar com.possystem.tools.SeedEmployees <path-to-sql-file>
 *
 * Not wired into the app's UI; this is a throwaway data-seeding helper.
 */
public class SeedEmployees {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: SeedEmployees <path-to-sql-file>");
            System.exit(1);
        }
        String path = args[0];
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("--")) continue;
                sb.append(line).append('\n');
            }
        }

        String[] statements = sb.toString().split(";\\s*\\n");
        int totalInserted = 0;
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            for (String raw : statements) {
                String sql = raw.trim();
                if (sql.isEmpty()) continue;
                int rows = st.executeUpdate(sql);
                totalInserted += rows;
                System.out.println("Executed statement, rows affected: " + rows);
            }
        }
        System.out.println("DONE. Total rows inserted: " + totalInserted);
    }
}
