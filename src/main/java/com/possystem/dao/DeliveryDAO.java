package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Delivery;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeliveryDAO {

    /** Creates the UNASSIGNED delivery row for an order placed as order_type = DELIVERY. */
    public int createDelivery(int orderId, String customerAddress) {
        String sql = "INSERT INTO deliveries (order_id, customer_address, status) VALUES (?, ?, 'UNASSIGNED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setString(2, customerAddress);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create delivery: " + e.getMessage(), e);
        }
        return -1;
    }

    /** Deliveries not yet completed, oldest first — the working queue for delivery staff. */
    public List<Delivery> getOpenQueue() {
        return getDeliveries("d.status != 'DELIVERED'");
    }

    /** Full delivery history, most recent first. */
    public List<Delivery> getAllDeliveries() {
        return getDeliveries(null);
    }

    private List<Delivery> getDeliveries(String whereClause) {
        List<Delivery> list = new ArrayList<>();
        String sql = "SELECT d.*, o.total AS order_total, c.name AS customer_name, c.phone AS customer_phone " +
                     "FROM deliveries d " +
                     "JOIN orders o ON d.order_id = o.id " +
                     "LEFT JOIN customers c ON o.customer_id = c.id " +
                     (whereClause != null ? "WHERE " + whereClause + " " : "") +
                     "ORDER BY d.created_at " + (whereClause != null ? "ASC" : "DESC");
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load deliveries: " + e.getMessage(), e);
        }
        return list;
    }

    public void assign(int deliveryId, String assignedTo) {
        String sql = "UPDATE deliveries SET assigned_to = ?, status = 'ASSIGNED' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assignedTo);
            ps.setInt(2, deliveryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to assign delivery: " + e.getMessage(), e);
        }
    }

    public void updateStatus(int deliveryId, String status) {
        String sql = "UPDATE deliveries SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, deliveryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update delivery status: " + e.getMessage(), e);
        }
    }

    private Delivery mapRow(ResultSet rs) throws SQLException {
        Delivery d = new Delivery();
        d.setId(rs.getInt("id"));
        d.setOrderId(rs.getInt("order_id"));
        d.setCustomerAddress(rs.getString("customer_address"));
        d.setAssignedTo(rs.getString("assigned_to"));
        d.setStatus(rs.getString("status"));
        d.setCreatedAt(rs.getTimestamp("created_at"));
        d.setOrderTotal(rs.getBigDecimal("order_total"));
        d.setCustomerName(rs.getString("customer_name"));
        d.setCustomerPhone(rs.getString("customer_phone"));
        return d;
    }
}
