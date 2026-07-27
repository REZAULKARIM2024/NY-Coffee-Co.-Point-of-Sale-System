package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class InspectRecipes {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                "SELECT c.id, c.name, p.name AS dept, c.station FROM categories c LEFT JOIN categories p ON c.parent_id = p.id ORDER BY dept, c.name")) {
                System.out.println("--- category -> department mapping ---");
                while (rs.next()) {
                    System.out.println(rs.getInt(1) + " | " + rs.getString(2) + " | dept=" + rs.getString(3) + " | station=" + rs.getString(4));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT id, name, unit FROM ingredients ORDER BY id")) {
                System.out.println("--- existing ingredients ---");
                while (rs.next()) System.out.println(rs.getInt(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
            }
        }
    }
}
