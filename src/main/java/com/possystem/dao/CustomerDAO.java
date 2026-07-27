package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Customer;

import java.sql.*;

public class CustomerDAO {

    public Customer findByPhone(String phone) {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up customer: " + e.getMessage(), e);
        }
        return null;
    }

    /** Looks the customer up by phone; creates a new record if they're new. */
    public Customer findOrCreate(String name, String phone, String email) {
        Customer existing = findByPhone(phone);
        if (existing != null) return existing;

        String sql = "INSERT INTO customers (name, phone, email, loyalty_points) VALUES (?,?,?,0)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                Customer c = new Customer();
                c.setId(keys.getInt(1));
                c.setName(name);
                c.setPhone(phone);
                c.setEmail(email);
                c.setLoyaltyPoints(0);
                return c;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create customer: " + e.getMessage(), e);
        }
    }

    public void updatePoints(int customerId, int points) {
        String sql = "UPDATE customers SET loyalty_points = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update loyalty points: " + e.getMessage(), e);
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setLoyaltyPoints(rs.getInt("loyalty_points"));
        return c;
    }
}
