package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * One-off migration: creates the new tables backing the Functions tab / Manager sidebar
 * (till management, drawer & safe transactions, app settings, notifications, training log,
 * batch operations, order flags) and adds a barcode column to menu_items. Idempotent - safe
 * to run more than once.
 */
public class MigrateOperations {

    private static final String[] STATEMENTS = {
        "CREATE TABLE IF NOT EXISTS app_settings (" +
            "setting_key VARCHAR(50) PRIMARY KEY, setting_value VARCHAR(255))",

        "CREATE TABLE IF NOT EXISTS tills (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE)",

        "CREATE TABLE IF NOT EXISTS till_assignments (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, till_id INT NOT NULL, employee_id INT NOT NULL, " +
            "register_name VARCHAR(50), assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "unassigned_at TIMESTAMP NULL, " +
            "FOREIGN KEY (till_id) REFERENCES tills(id), FOREIGN KEY (employee_id) REFERENCES employees(id))",

        "CREATE TABLE IF NOT EXISTS till_counts (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, till_id INT NOT NULL, counted_amount DECIMAL(10,2) NOT NULL, " +
            "expected_amount DECIMAL(10,2), variance DECIMAL(10,2), counted_by INT NULL, " +
            "counted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, notes VARCHAR(255), " +
            "FOREIGN KEY (till_id) REFERENCES tills(id), FOREIGN KEY (counted_by) REFERENCES employees(id))",

        "CREATE TABLE IF NOT EXISTS drawer_transactions (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "type ENUM('PAID_IN','PAID_OUT','CASH_PULL','NO_SALE') NOT NULL, " +
            "amount DECIMAL(10,2) NOT NULL DEFAULT 0, reason VARCHAR(255), employee_id INT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (employee_id) REFERENCES employees(id))",

        "CREATE TABLE IF NOT EXISTS safe_transactions (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "type ENUM('OPEN','ADD_FUNDS','COUNT','CLOSE','DEPOSIT') NOT NULL, " +
            "amount DECIMAL(10,2) NOT NULL DEFAULT 0, employee_id INT NULL, notes VARCHAR(255), " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (employee_id) REFERENCES employees(id))",

        "CREATE TABLE IF NOT EXISTS notifications (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, message VARCHAR(255) NOT NULL, " +
            "is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",

        "CREATE TABLE IF NOT EXISTS employee_training (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, topic VARCHAR(100) NOT NULL, " +
            "completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (employee_id) REFERENCES employees(id))",

        "CREATE TABLE IF NOT EXISTS batch_operations (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, type VARCHAR(50) NOT NULL, item_count INT NOT NULL DEFAULT 0, " +
            "initiated_by INT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (initiated_by) REFERENCES employees(id))",

        "CREATE TABLE IF NOT EXISTS order_flags (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, order_id INT NOT NULL, " +
            "flag ENUM('READY','RECALLED') NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (order_id) REFERENCES orders(id))"
    };

    public static void main(String[] args) throws Exception {
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            for (String sql : STATEMENTS) {
                st.executeUpdate(sql);
                System.out.println("OK: " + sql.substring(0, Math.min(60, sql.length())) + "...");
            }

            if (!columnExists(conn, "menu_items", "barcode")) {
                st.executeUpdate("ALTER TABLE menu_items ADD COLUMN barcode VARCHAR(30) NULL UNIQUE");
                System.out.println("OK: added menu_items.barcode column");
            } else {
                System.out.println("SKIP: menu_items.barcode already exists");
            }

            // Seed a couple of tills so Till Management has something to assign, if empty.
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM tills")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    st.executeUpdate("INSERT INTO tills (name) VALUES ('Till 1'), ('Till 2'), ('Till 3')");
                    System.out.println("OK: seeded default tills (Till 1-3)");
                }
            }
        }
        System.out.println("DONE.");
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                     "AND table_name = ? AND column_name = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
