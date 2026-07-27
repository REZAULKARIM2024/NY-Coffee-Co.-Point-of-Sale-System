package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Modifier;
import com.possystem.model.Size;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PricingOptionsDAO {

    public List<Size> getAllSizes() {
        List<Size> list = new ArrayList<>();
        String sql = "SELECT * FROM sizes ORDER BY sort_order";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Size(rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("price_delta")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load sizes: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Modifier> getAllModifiers() {
        List<Modifier> list = new ArrayList<>();
        String sql = "SELECT * FROM modifiers ORDER BY sort_order";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Modifier(rs.getInt("id"), rs.getString("name"),
                    rs.getBigDecimal("price_delta"), rs.getString("modifier_group")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load modifiers: " + e.getMessage(), e);
        }
        return list;
    }
}
