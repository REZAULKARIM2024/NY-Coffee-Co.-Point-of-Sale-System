package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.RecipeStep;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeDAO {

    /** Steps for one menu item in one language, in order. */
    public List<RecipeStep> getSteps(int menuItemId, String language) {
        List<RecipeStep> list = new ArrayList<>();
        String sql = "SELECT * FROM recipe_steps WHERE menu_item_id = ? AND language = ? ORDER BY step_number";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setString(2, language);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recipe steps: " + e.getMessage(), e);
        }
        return list;
    }

    /** Which languages already have at least one step written for this item, e.g. ["en", "es"]. */
    public List<String> getLanguagesForItem(int menuItemId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT language FROM recipe_steps WHERE menu_item_id = ? ORDER BY language";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("language"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recipe languages: " + e.getMessage(), e);
        }
        return list;
    }

    /** Appends a new step at the end of this item+language's list. */
    public void addStep(int menuItemId, String language, String instruction) {
        String nextNumSql = "SELECT COALESCE(MAX(step_number), 0) + 1 FROM recipe_steps WHERE menu_item_id = ? AND language = ?";
        String insertSql = "INSERT INTO recipe_steps (menu_item_id, language, step_number, instruction) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection()) {
            int nextNum;
            try (PreparedStatement ps = conn.prepareStatement(nextNumSql)) {
                ps.setInt(1, menuItemId);
                ps.setString(2, language);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    nextNum = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, menuItemId);
                ps.setString(2, language);
                ps.setInt(3, nextNum);
                ps.setString(4, instruction);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add recipe step: " + e.getMessage(), e);
        }
    }

    public void updateStepText(int stepId, String instruction) {
        String sql = "UPDATE recipe_steps SET instruction = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instruction);
            ps.setInt(2, stepId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update recipe step: " + e.getMessage(), e);
        }
    }

    public void deleteStep(int stepId) {
        String sql = "DELETE FROM recipe_steps WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stepId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete recipe step: " + e.getMessage(), e);
        }
    }

    private RecipeStep mapRow(ResultSet rs) throws SQLException {
        RecipeStep s = new RecipeStep();
        s.setId(rs.getInt("id"));
        s.setMenuItemId(rs.getInt("menu_item_id"));
        s.setLanguage(rs.getString("language"));
        s.setStepNumber(rs.getInt("step_number"));
        s.setInstruction(rs.getString("instruction"));
        return s;
    }
}
