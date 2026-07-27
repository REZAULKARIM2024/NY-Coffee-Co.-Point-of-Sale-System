package com.possystem.dao;

import com.possystem.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Backs the Functions tab / Manager sidebar: app-wide toggle settings, till assignment &
 * counting, cash drawer & safe transactions, notifications, employee training log, batch
 * operations, order flags (DT/OTG workflow), and the manager Reports screens. Consolidated
 * into one DAO since these are all small, closely-related "back office operations" concerns.
 */
public class OperationsDAO {

    // ---------- APP SETTINGS (simple key/value store for toggles like KDS mode, SmartSell) ----------

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read setting: " + e.getMessage(), e);
        }
        return defaultValue;
    }

    public void setSetting(String key, String value) {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save setting: " + e.getMessage(), e);
        }
    }

    // ---------- TILLS ----------

    /** [id, name] for every till. */
    public List<Object[]> getAllTills() {
        return queryRows("SELECT id, name FROM tills ORDER BY name", 2);
    }

    /** Tills with no currently-open assignment (unassigned_at IS NULL). */
    public List<Object[]> getUnassignedTills() {
        String sql = "SELECT t.id, t.name FROM tills t WHERE NOT EXISTS " +
                     "(SELECT 1 FROM till_assignments a WHERE a.till_id = t.id AND a.unassigned_at IS NULL) ORDER BY t.name";
        return queryRows(sql, 2);
    }

    /** [tillName, employeeName, registerName, assignedAt] for every currently-open assignment. */
    public List<Object[]> getActiveTillAssignments() {
        String sql = "SELECT t.name, e.full_name, a.register_name, a.assigned_at, a.id " +
                     "FROM till_assignments a JOIN tills t ON a.till_id = t.id " +
                     "JOIN employees e ON a.employee_id = e.id WHERE a.unassigned_at IS NULL ORDER BY a.assigned_at";
        return queryRows(sql, 5);
    }

    public void assignTill(int tillId, int employeeId, String registerName) {
        String sql = "INSERT INTO till_assignments (till_id, employee_id, register_name) VALUES (?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tillId);
            ps.setInt(2, employeeId);
            ps.setString(3, registerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to assign till: " + e.getMessage(), e);
        }
    }

    /** Closes every open assignment (unassigned_at IS NULL) for this till or employee. */
    public int unassignTill(Integer tillId, Integer employeeId) {
        StringBuilder sql = new StringBuilder("UPDATE till_assignments SET unassigned_at = NOW() WHERE unassigned_at IS NULL");
        List<Object> params = new ArrayList<>();
        if (tillId != null) { sql.append(" AND till_id = ?"); params.add(tillId); }
        if (employeeId != null) { sql.append(" AND employee_id = ?"); params.add(employeeId); }
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to unassign till: " + e.getMessage(), e);
        }
    }

    public void recordTillCount(int tillId, BigDecimal counted, BigDecimal expected, Integer countedByEmployeeId, String notes) {
        BigDecimal variance = counted.subtract(expected == null ? BigDecimal.ZERO : expected);
        String sql = "INSERT INTO till_counts (till_id, counted_amount, expected_amount, variance, counted_by, notes) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tillId);
            ps.setBigDecimal(2, counted);
            ps.setBigDecimal(3, expected == null ? BigDecimal.ZERO : expected);
            ps.setBigDecimal(4, variance);
            if (countedByEmployeeId != null) ps.setInt(5, countedByEmployeeId); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record till count: " + e.getMessage(), e);
        }
    }

    /** [tillName, countedAmount, expectedAmount, variance, countedBy, countedAt]. */
    public List<Object[]> getTillCountHistory() {
        String sql = "SELECT t.name, c.counted_amount, c.expected_amount, c.variance, " +
                     "COALESCE(e.full_name, 'N/A'), c.counted_at " +
                     "FROM till_counts c JOIN tills t ON c.till_id = t.id " +
                     "LEFT JOIN employees e ON c.counted_by = e.id ORDER BY c.counted_at DESC LIMIT 100";
        return queryRows(sql, 6);
    }

    // ---------- DRAWER TRANSACTIONS (Paid In / Paid Out / Cash Pull / No Sale) ----------

    public void recordDrawerTransaction(String type, BigDecimal amount, String reason, Integer employeeId) {
        String sql = "INSERT INTO drawer_transactions (type, amount, reason, employee_id) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setBigDecimal(2, amount == null ? BigDecimal.ZERO : amount);
            ps.setString(3, reason);
            if (employeeId != null) ps.setInt(4, employeeId); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record drawer transaction: " + e.getMessage(), e);
        }
    }

    /** [type, amount, reason, employeeName, createdAt] for today only. */
    public List<Object[]> getTodayDrawerTransactions() {
        String sql = "SELECT d.type, d.amount, d.reason, COALESCE(e.full_name, 'N/A'), d.created_at " +
                     "FROM drawer_transactions d LEFT JOIN employees e ON d.employee_id = e.id " +
                     "WHERE DATE(d.created_at) = CURDATE() ORDER BY d.created_at DESC";
        return queryRows(sql, 5);
    }

    public List<Object[]> getAllDrawerTransactions() {
        String sql = "SELECT d.type, d.amount, d.reason, COALESCE(e.full_name, 'N/A'), d.created_at " +
                     "FROM drawer_transactions d LEFT JOIN employees e ON d.employee_id = e.id " +
                     "ORDER BY d.created_at DESC LIMIT 200";
        return queryRows(sql, 5);
    }

    public BigDecimal sumTodayDrawerTransactions(String type) {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM drawer_transactions WHERE type = ? AND DATE(created_at) = CURDATE()";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum drawer transactions: " + e.getMessage(), e);
        }
    }

    // ---------- SAFE TRANSACTIONS ----------

    public void recordSafeTransaction(String type, BigDecimal amount, Integer employeeId, String notes) {
        String sql = "INSERT INTO safe_transactions (type, amount, employee_id, notes) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setBigDecimal(2, amount == null ? BigDecimal.ZERO : amount);
            if (employeeId != null) ps.setInt(3, employeeId); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record safe transaction: " + e.getMessage(), e);
        }
    }

    public List<Object[]> getAllSafeTransactions() {
        String sql = "SELECT s.type, s.amount, s.notes, COALESCE(e.full_name, 'N/A'), s.created_at " +
                     "FROM safe_transactions s LEFT JOIN employees e ON s.employee_id = e.id " +
                     "ORDER BY s.created_at DESC LIMIT 200";
        return queryRows(sql, 5);
    }

    public BigDecimal currentSafeBalance() {
        String sql = "SELECT COALESCE(SUM(CASE WHEN type IN ('ADD_FUNDS','DEPOSIT') THEN amount " +
                     "WHEN type IN ('CLOSE') THEN -amount ELSE 0 END), 0) FROM safe_transactions";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getBigDecimal(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute safe balance: " + e.getMessage(), e);
        }
    }

    // ---------- NOTIFICATIONS ----------

    public void addNotification(String message) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO notifications (message) VALUES (?)")) {
            ps.setString(1, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add notification: " + e.getMessage(), e);
        }
    }

    /** [id, message, isRead, createdAt]. */
    public List<Object[]> getNotifications() {
        return queryRows("SELECT id, message, is_read, created_at FROM notifications ORDER BY created_at DESC LIMIT 50", 4);
    }

    public void markNotificationRead(int id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET is_read = TRUE WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update notification: " + e.getMessage(), e);
        }
    }

    // ---------- EMPLOYEE TRAINING ----------

    public void recordTraining(int employeeId, String topic) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO employee_training (employee_id, topic) VALUES (?,?)")) {
            ps.setInt(1, employeeId);
            ps.setString(2, topic);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record training: " + e.getMessage(), e);
        }
    }

    /** [employeeName, topic, completedAt]. */
    public List<Object[]> getTrainingRecords() {
        String sql = "SELECT e.full_name, t.topic, t.completed_at FROM employee_training t " +
                     "JOIN employees e ON t.employee_id = e.id ORDER BY t.completed_at DESC LIMIT 100";
        return queryRows(sql, 3);
    }

    // ---------- BATCH OPERATIONS ----------

    public void recordBatchOperation(String type, int itemCount, Integer employeeId) {
        String sql = "INSERT INTO batch_operations (type, item_count, initiated_by) VALUES (?,?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, itemCount);
            if (employeeId != null) ps.setInt(3, employeeId); else ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record batch operation: " + e.getMessage(), e);
        }
    }

    // ---------- ORDER FLAGS (DT/OTG workflow) ----------

    public void addOrderFlag(int orderId, String flag) {
        String sql = "INSERT INTO order_flags (order_id, flag) VALUES (?,?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, flag);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to flag order: " + e.getMessage(), e);
        }
    }

    public boolean hasOrderFlag(int orderId, String flag) {
        String sql = "SELECT 1 FROM order_flags WHERE order_id = ? AND flag = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, flag);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check order flag: " + e.getMessage(), e);
        }
    }

    // ---------- MANAGER REPORTS ----------

    /** [employeeName, position, periodsRun, totalGross, totalNet]. */
    public List<Object[]> employeeFinancialReport() {
        String sql = "SELECT e.full_name, e.position, COUNT(*), COALESCE(SUM(p.gross_pay),0), COALESCE(SUM(p.net_pay),0) " +
                     "FROM payroll_runs p JOIN employees e ON p.employee_id = e.id " +
                     "GROUP BY e.id, e.full_name, e.position ORDER BY SUM(p.net_pay) DESC LIMIT 100";
        return queryRows(sql, 5);
    }

    /** Single summary row set: [metric, value] for overall store financials (last 30 days). */
    public List<Object[]> propertyFinancialReport() {
        String sql = "SELECT COUNT(*), COALESCE(SUM(subtotal),0), COALESCE(SUM(discount),0), " +
                     "COALESCE(SUM(tax),0), COALESCE(SUM(total),0) " +
                     "FROM orders WHERE status = 'COMPLETED' AND created_at >= NOW() - INTERVAL 30 DAY";
        List<Object[]> out = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                out.add(new Object[]{"Completed Orders (30d)", rs.getInt(1)});
                out.add(new Object[]{"Gross Subtotal", rs.getBigDecimal(2)});
                out.add(new Object[]{"Discounts Given", rs.getBigDecimal(3)});
                out.add(new Object[]{"Tax Collected", rs.getBigDecimal(4)});
                out.add(new Object[]{"Net Revenue", rs.getBigDecimal(5)});
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to build property financial report: " + e.getMessage(), e);
        }
        return out;
    }

    /** [itemName, category, qtySold, revenue] — top sellers by revenue, last 30 days. */
    public List<Object[]> menuItemSummaryReport() {
        String sql = "SELECT m.name, c.name, COALESCE(SUM(oi.quantity),0), COALESCE(SUM(oi.quantity*oi.unit_price),0) " +
                     "FROM order_items oi JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "JOIN orders o ON oi.order_id = o.id " +
                     "WHERE o.created_at >= NOW() - INTERVAL 30 DAY " +
                     "GROUP BY m.id, m.name, c.name ORDER BY SUM(oi.quantity*oi.unit_price) DESC LIMIT 50";
        return queryRows(sql, 4);
    }

    /** Same shape as menuItemSummaryReport but ordered by quantity (units sold) instead of revenue. */
    public List<Object[]> menuItemSalesReport() {
        String sql = "SELECT m.name, c.name, COALESCE(SUM(oi.quantity),0), COALESCE(SUM(oi.quantity*oi.unit_price),0) " +
                     "FROM order_items oi JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "JOIN orders o ON oi.order_id = o.id " +
                     "WHERE o.created_at >= NOW() - INTERVAL 30 DAY " +
                     "GROUP BY m.id, m.name, c.name ORDER BY SUM(oi.quantity) DESC LIMIT 50";
        return queryRows(sql, 4);
    }

    /** [departmentName, qtySold, revenue] grouped by top-level department (parent_id IS NULL). */
    public List<Object[]> familyGroupSalesReport() {
        String sql = "SELECT dept.name, COALESCE(SUM(oi.quantity),0), COALESCE(SUM(oi.quantity*oi.unit_price),0) " +
                     "FROM order_items oi JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "JOIN categories dept ON COALESCE(c.parent_id, c.id) = dept.id " +
                     "JOIN orders o ON oi.order_id = o.id " +
                     "WHERE o.created_at >= NOW() - INTERVAL 30 DAY " +
                     "GROUP BY dept.id, dept.name ORDER BY SUM(oi.quantity*oi.unit_price) DESC";
        return queryRows(sql, 3);
    }

    /** [categoryName, qtySold, revenue] grouped by leaf subcategory. */
    public List<Object[]> majorGroupSalesReport() {
        String sql = "SELECT c.name, COALESCE(SUM(oi.quantity),0), COALESCE(SUM(oi.quantity*oi.unit_price),0) " +
                     "FROM order_items oi JOIN menu_items m ON oi.menu_item_id = m.id " +
                     "JOIN categories c ON m.category_id = c.id " +
                     "JOIN orders o ON oi.order_id = o.id " +
                     "WHERE o.created_at >= NOW() - INTERVAL 30 DAY " +
                     "GROUP BY c.id, c.name ORDER BY SUM(oi.quantity*oi.unit_price) DESC LIMIT 50";
        return queryRows(sql, 3);
    }

    /** [employeeName, clockInTime] for everyone currently clocked in. */
    public List<Object[]> clockInStatusReport() {
        String sql = "SELECT e.full_name, tc.clock_in FROM time_clock tc JOIN employees e ON tc.employee_id = e.id " +
                     "WHERE tc.clock_out IS NULL ORDER BY tc.clock_in";
        return queryRows(sql, 2);
    }

    /** [employeeName, totalHours] over the last 30 days of closed shifts. */
    public List<Object[]> employeeLaborSummaryReport() {
        String sql = "SELECT e.full_name, COALESCE(SUM(TIMESTAMPDIFF(MINUTE, tc.clock_in, tc.clock_out)) / 60.0, 0) " +
                     "FROM time_clock tc JOIN employees e ON tc.employee_id = e.id " +
                     "WHERE tc.clock_out IS NOT NULL AND tc.clock_in >= NOW() - INTERVAL 30 DAY " +
                     "GROUP BY e.id, e.full_name ORDER BY 2 DESC LIMIT 100";
        return queryRows(sql, 2);
    }

    /** [date, totalHours] over the last 14 days, across all employees. */
    public List<Object[]> timePeriodSummaryReport() {
        String sql = "SELECT DATE(tc.clock_in), COALESCE(SUM(TIMESTAMPDIFF(MINUTE, tc.clock_in, tc.clock_out)) / 60.0, 0) " +
                     "FROM time_clock tc WHERE tc.clock_out IS NOT NULL AND tc.clock_in >= NOW() - INTERVAL 14 DAY " +
                     "GROUP BY DATE(tc.clock_in) ORDER BY 1 DESC";
        return queryRows(sql, 2);
    }

    /** [orderId, cashierName, total, createdAt] for the most recent completed checks. */
    public List<Object[]> employeeClosedCheckReport() {
        String sql = "SELECT o.id, u.full_name, o.total, o.created_at FROM orders o " +
                     "JOIN users u ON o.user_id = u.id WHERE o.status = 'COMPLETED' " +
                     "ORDER BY o.created_at DESC LIMIT 100";
        return queryRows(sql, 4);
    }

    /** [orderId, cashierName, total, createdAt] for currently-open checks. */
    public List<Object[]> employeeOpenCheckReport() {
        String sql = "SELECT o.id, u.full_name, o.total, o.created_at FROM orders o " +
                     "JOIN users u ON o.user_id = u.id WHERE o.status = 'OPEN' " +
                     "ORDER BY o.created_at DESC LIMIT 100";
        return queryRows(sql, 4);
    }

    /** [ingredientName, stockQty, threshold] for every ingredient currently below its low-stock
     *  threshold — used to populate the Notification Center with real, live alerts. */
    public List<Object[]> lowStockIngredients() {
        String sql = "SELECT name, stock_quantity, low_stock_threshold FROM ingredients " +
                     "WHERE stock_quantity < low_stock_threshold ORDER BY name";
        return queryRows(sql, 3);
    }

    // ---------- shared helper ----------

    private List<Object[]> queryRows(String sql, int colCount) {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 0; i < colCount; i++) row[i] = rs.getObject(i + 1);
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
        return list;
    }
}
