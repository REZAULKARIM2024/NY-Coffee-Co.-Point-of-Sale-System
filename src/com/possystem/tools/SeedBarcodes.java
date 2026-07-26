package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * One-off: assigns a deterministic 12-digit UPC-style barcode ("2" + zero-padded item id (10
 * digits) + check digit) to every menu item that doesn't have one yet, so the Barcode Entry
 * function has real data to look up.
 */
public class SeedBarcodes {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            int updated = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM menu_items WHERE barcode IS NULL")) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE menu_items SET barcode = ? WHERE id = ?")) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String barcode = makeBarcode(id);
                        ps.setString(1, barcode);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                        updated++;
                    }
                }
            }
            System.out.println("Barcodes assigned: " + updated);
        }
        System.out.println("DONE.");
    }

    private static String makeBarcode(int id) {
        String body = "2" + String.format("%010d", id); // 11 digits
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            int digit = body.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }
        int check = (10 - (sum % 10)) % 10;
        return body + check; // 12 digits total
    }
}
