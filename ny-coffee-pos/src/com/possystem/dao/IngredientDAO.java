package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Ingredient;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IngredientDAO {

    public List<Ingredient> getAllIngredients() {
        List<Ingredient> list = new ArrayList<>();
        String sql = "SELECT * FROM ingredients ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load ingredients: " + e.getMessage(), e);
        }
        return list;
    }

    /** Manual stock adjustment (purchase-in, waste, correction) with audit log entry. */
    public void adjustStock(int ingredientId, BigDecimal changeAmount, String reason, Integer userId) {
        String updateSql = "UPDATE ingredients SET stock_quantity = stock_quantity + ? WHERE id = ?";
        String logSql = "INSERT INTO inventory_transactions (ingredient_id, change_amount, reason, user_id) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateSql)) {
                ps1.setBigDecimal(1, changeAmount);
                ps1.setInt(2, ingredientId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(logSql)) {
                ps2.setInt(1, ingredientId);
                ps2.setBigDecimal(2, changeAmount);
                ps2.setString(3, reason);
                if (userId != null) ps2.setInt(4, userId); else ps2.setNull(4, Types.INTEGER);
                ps2.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Stock adjustment failed: " + e.getMessage(), e);
        }
    }

    private Ingredient mapRow(ResultSet rs) throws SQLException {
        Ingredient i = new Ingredient();
        i.setId(rs.getInt("id"));
        i.setName(rs.getString("name"));
        i.setUnit(rs.getString("unit"));
        i.setStockQuantity(rs.getBigDecimal("stock_quantity"));
        i.setLowStockThreshold(rs.getBigDecimal("low_stock_threshold"));
        i.setUnitCost(rs.getBigDecimal("unit_cost"));
        return i;
    }
}
