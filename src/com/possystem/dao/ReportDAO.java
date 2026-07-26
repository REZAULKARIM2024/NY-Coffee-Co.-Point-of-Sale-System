package com.possystem.dao;

import com.possystem.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;

public class ReportDAO {

    public static class DailySummary {
        public int orderCount;
        public BigDecimal totalSales = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal totalDiscount = BigDecimal.ZERO;
    }

    public DailySummary getTodaySummary() {
        String sql = "SELECT COUNT(*) AS cnt, COALESCE(SUM(total),0) AS total, " +
                     "COALESCE(SUM(tax),0) AS tax, COALESCE(SUM(discount),0) AS discount " +
                     "FROM orders WHERE status='COMPLETED' AND DATE(created_at) = CURDATE()";
        DailySummary summary = new DailySummary();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                summary.orderCount = rs.getInt("cnt");
                summary.totalSales = rs.getBigDecimal("total");
                summary.totalTax = rs.getBigDecimal("tax");
                summary.totalDiscount = rs.getBigDecimal("discount");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load today's summary: " + e.getMessage(), e);
        }
        return summary;
    }

    public static class ItemSales {
        public String itemName;
        public int quantitySold;
        public BigDecimal revenue;
    }

    public java.util.List<ItemSales> getTopItemsToday(int limit) {
        String sql = "SELECT m.name, SUM(oi.quantity) AS qty, SUM(oi.quantity * oi.unit_price) AS revenue " +
                     "FROM order_items oi " +
                     "JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "JOIN orders o ON oi.order_id = o.id " +
                     "WHERE o.status='COMPLETED' AND DATE(o.created_at) = CURDATE() " +
                     "GROUP BY m.id, m.name ORDER BY revenue DESC LIMIT ?";
        java.util.List<ItemSales> list = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemSales item = new ItemSales();
                    item.itemName = rs.getString("name");
                    item.quantitySold = rs.getInt("qty");
                    item.revenue = rs.getBigDecimal("revenue");
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load top items: " + e.getMessage(), e);
        }
        return list;
    }
}
