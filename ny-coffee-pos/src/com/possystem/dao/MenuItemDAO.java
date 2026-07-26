package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Category;
import com.possystem.model.MenuItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY sort_order";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapCategoryRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load categories: " + e.getMessage(), e);
        }
        return list;
    }

    /** Top-level department tabs (Beverages, Bakery, Sandwiches, Retail, Local, ...). */
    public List<Category> getTopLevelCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL ORDER BY sort_order";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapCategoryRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load top-level categories: " + e.getMessage(), e);
        }
        return list;
    }

    /** Subcategories shown in the sidebar under a given department. Empty list = department has no sidebar. */
    public List<Category> getChildCategories(int parentId) {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE parent_id = ? ORDER BY sort_order";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCategoryRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load subcategories: " + e.getMessage(), e);
        }
        return list;
    }

    private Category mapCategoryRow(ResultSet rs) throws SQLException {
        Category c = new Category(rs.getInt("id"), rs.getString("name"), rs.getString("station"));
        c.setSortOrder(rs.getInt("sort_order"));
        int parentId = rs.getInt("parent_id");
        c.setParentId(rs.wasNull() ? null : parentId);
        return c;
    }

    public List<MenuItem> getActiveMenuItems() {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, c.name AS category_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id = c.id WHERE m.active = TRUE ORDER BY c.sort_order, m.name";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load menu items: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Active items for one category (a subcategory such as "Coffee", or a department that has
     * no subcategories), ordered so items with the same `section` label group together for the
     * POS item grid, in the section's first-appearance order, then alphabetically within it.
     */
    public List<MenuItem> getMenuItemsByCategory(int categoryId) {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, c.name AS category_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "WHERE m.active = TRUE AND m.category_id = ? " +
                     "ORDER BY (m.section IS NULL), m.id, m.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load menu items for category: " + e.getMessage(), e);
        }
        return list;
    }

    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, c.name AS category_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id = c.id ORDER BY c.sort_order, m.name";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load menu items: " + e.getMessage(), e);
        }
        return list;
    }

    public void saveMenuItem(MenuItem item) {
        if (item.getId() == 0) {
            String sql = "INSERT INTO menu_items (category_id, name, description, price, cost, image_path, section, active) VALUES (?,?,?,?,?,?,?,?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindItem(ps, item);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) item.setId(keys.getInt(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to insert menu item: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE menu_items SET category_id=?, name=?, description=?, price=?, cost=?, image_path=?, section=?, active=? WHERE id=?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                bindItem(ps, item);
                ps.setInt(9, item.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update menu item: " + e.getMessage(), e);
            }
        }
    }

    private void bindItem(PreparedStatement ps, MenuItem item) throws SQLException {
        ps.setInt(1, item.getCategoryId());
        ps.setString(2, item.getName());
        ps.setString(3, item.getDescription());
        ps.setBigDecimal(4, item.getPrice());
        ps.setBigDecimal(5, item.getCost() == null ? BigDecimal.ZERO : item.getCost());
        ps.setString(6, item.getImagePath());
        ps.setString(7, item.getSection());
        ps.setBoolean(8, item.isActive());
    }

    /** Looks up an active item by its scanned barcode (Barcode Entry function). Null if not found. */
    public MenuItem findByBarcode(String barcode) {
        String sql = "SELECT m.*, c.name AS category_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id = c.id WHERE m.barcode = ? AND m.active = TRUE";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Barcode lookup failed: " + e.getMessage(), e);
        }
        return null;
    }

    /** Direct price update (Menu Item Price Override function) — bypasses the full save form. */
    public void updatePrice(int menuItemId, BigDecimal newPrice) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE menu_items SET price = ? WHERE id = ?")) {
            ps.setBigDecimal(1, newPrice);
            ps.setInt(2, menuItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to override price: " + e.getMessage(), e);
        }
    }

    /** Direct active-flag update (Menu Item Availability function). */
    public void setActive(int menuItemId, boolean active) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE menu_items SET active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, menuItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update availability: " + e.getMessage(), e);
        }
    }

    private MenuItem mapRow(ResultSet rs) throws SQLException {
        MenuItem m = new MenuItem();
        m.setId(rs.getInt("id"));
        m.setCategoryId(rs.getInt("category_id"));
        m.setCategoryName(rs.getString("category_name"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setPrice(rs.getBigDecimal("price"));
        m.setCost(rs.getBigDecimal("cost"));
        m.setImagePath(rs.getString("image_path"));
        m.setSection(rs.getString("section"));
        m.setActive(rs.getBoolean("active"));
        return m;
    }
}
