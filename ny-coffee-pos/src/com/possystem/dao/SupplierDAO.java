package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public List<Supplier> getAllSuppliers() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load suppliers: " + e.getMessage(), e);
        }
        return list;
    }

    /** Inserts a new supplier (id == 0) or updates an existing one. */
    public void saveSupplier(Supplier s) {
        if (s.getId() == 0) {
            String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?,?,?,?,?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, s);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) s.setId(keys.getInt(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add supplier: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? WHERE id=?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, s);
                ps.setInt(6, s.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update supplier: " + e.getMessage(), e);
            }
        }
    }

    public void deleteSupplier(int id) {
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete supplier: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getContactPerson());
        ps.setString(3, s.getPhone());
        ps.setString(4, s.getEmail());
        ps.setString(5, s.getAddress());
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        return s;
    }
}
