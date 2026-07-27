package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.CartItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

public class OrderDAO {

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) { super(message); }
    }

    /** One leg of a (possibly split) payment: e.g. $2.00 CASH + $3.94 CARD on the same order. */
    public static class PaymentPart {
        public final String method;
        public final BigDecimal amount;
        public final String reference;
        public PaymentPart(String method, BigDecimal amount, String reference) {
            this.method = method;
            this.amount = amount;
            this.reference = reference;
        }
    }

    /**
     * Creates the order, its line items, a payment row, and deducts ingredient stock
     * for every item sold — all inside a single DB transaction so a failure anywhere
     * rolls everything back (no half-completed sale, no silently wrong stock counts).
     *
     * @return the generated order id
     */
    public int checkout(List<CartItem> cart, int userId, Integer customerId,
                         BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total,
                         String paymentMethod, String paymentReference) {
        return checkout(cart, userId, customerId, subtotal, discount, tax, total,
                paymentMethod, paymentReference, "IN_STORE", "DINE_IN");
    }

    /**
     * Same as the 9-arg checkout(), but lets the caller record where the order came from
     * (IN_STORE/PHONE/ONLINE) and how it's being fulfilled (DINE_IN/PICKUP/DELIVERY).
     */
    public int checkout(List<CartItem> cart, int userId, Integer customerId,
                         BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total,
                         String paymentMethod, String paymentReference, String orderSource, String orderType) {
        return checkoutMultiPayment(cart, userId, customerId, subtotal, discount, tax, total,
                List.of(new PaymentPart(paymentMethod, total, paymentReference)), orderSource, orderType);
    }

    /**
     * Same as checkout(), but records the order total as two or more payment rows instead of
     * one — e.g. a split-tender sale where part of the total is paid in CASH and the rest is
     * charged to CARD. The `payments` amounts should sum to `total`; each becomes its own row
     * in the payments table against the same order.
     */
    public int checkoutMultiPayment(List<CartItem> cart, int userId, Integer customerId,
                         BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total,
                         List<PaymentPart> payments, String orderSource, String orderType) {

        String insertOrder = "INSERT INTO orders (order_source, order_type, customer_id, user_id, subtotal, discount, tax, total, status) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED')";
        String insertItem = "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price, is_loyalty_free) VALUES (?,?,?,?,?)";
        String insertPayment = "INSERT INTO payments (order_id, method, amount, status, reference_number) VALUES (?,?,?,?,?)";
        String checkStock = "SELECT ri.ingredient_id, ri.quantity_required * ? AS needed, i.stock_quantity, i.name " +
                             "FROM recipe_ingredients ri JOIN ingredients i ON ri.ingredient_id = i.id WHERE ri.menu_item_id = ?";
        String deductStock = "UPDATE ingredients SET stock_quantity = stock_quantity - ? WHERE id = ?";
        String logTxn = "INSERT INTO inventory_transactions (ingredient_id, change_amount, reason, reference_order_id, user_id) VALUES (?,?, 'SALE', ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, orderSource);
                ps.setString(2, orderType);
                if (customerId != null) ps.setInt(3, customerId); else ps.setNull(3, Types.INTEGER);
                ps.setInt(4, userId);
                ps.setBigDecimal(5, subtotal);
                ps.setBigDecimal(6, discount);
                ps.setBigDecimal(7, tax);
                ps.setBigDecimal(8, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
            }

            for (CartItem item : cart) {
                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.getMenuItem().getId());
                    ps.setInt(3, item.getQuantity());
                    ps.setBigDecimal(4, item.getUnitPrice());
                    ps.setBoolean(5, item.isLoyaltyFree());
                    ps.executeUpdate();
                }

                // Check & deduct stock per ingredient used by this menu item
                try (PreparedStatement ps = conn.prepareStatement(checkStock)) {
                    ps.setInt(1, item.getQuantity());
                    ps.setInt(2, item.getMenuItem().getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            BigDecimal needed = rs.getBigDecimal("needed");
                            BigDecimal available = rs.getBigDecimal("stock_quantity");
                            String ingredientName = rs.getString("name");
                            int ingredientId = rs.getInt("ingredient_id");

                            if (available.compareTo(needed) < 0) {
                                throw new InsufficientStockException(
                                    "Insufficient stock for item: " + ingredientName +
                                    " (need " + needed + ", have " + available + ")");
                            }

                            try (PreparedStatement deduct = conn.prepareStatement(deductStock)) {
                                deduct.setBigDecimal(1, needed);
                                deduct.setInt(2, ingredientId);
                                deduct.executeUpdate();
                            }
                            try (PreparedStatement log = conn.prepareStatement(logTxn)) {
                                log.setInt(1, ingredientId);
                                log.setBigDecimal(2, needed.negate());
                                log.setInt(3, orderId);
                                log.setInt(4, userId);
                                log.executeUpdate();
                            }
                        }
                    }
                }
            }

            for (PaymentPart part : payments) {
                try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                    ps.setInt(1, orderId);
                    ps.setString(2, part.method);
                    ps.setBigDecimal(3, part.amount);
                    ps.setString(4, "SUCCESS");
                    ps.setString(5, part.reference);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return orderId;

        } catch (InsufficientStockException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Checkout failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }

    // ---------- ORDER LISTS / MANAGER ACTIONS (Functions tab: Open Checks, Phone/DT/OTG Orders, Check Functions) ----------

    /** [orderId, sourceLabel, typeLabel, customerName, cashierName, total, createdAt] for open orders. */
    public List<Object[]> getOpenOrders() {
        return getOrdersByFilter("OPEN", null, null);
    }

    public List<Object[]> getOrdersByStatus(String status) {
        return getOrdersByFilter(status, null, null);
    }

    /** Generic filter: any of status/source/type may be null to mean "any". */
    public List<Object[]> getOrdersByFilter(String status, String source, String type) {
        StringBuilder sql = new StringBuilder(
            "SELECT o.id, o.order_source, o.order_type, COALESCE(c.name, 'Walk-in'), u.full_name, o.total, o.created_at " +
            "FROM orders o JOIN users u ON o.user_id = u.id LEFT JOIN customers c ON o.customer_id = c.id WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();
        if (status != null) { sql.append(" AND o.status = ?"); params.add(status); }
        if (source != null) { sql.append(" AND o.order_source = ?"); params.add(source); }
        if (type != null) { sql.append(" AND o.order_type = ?"); params.add(type); }
        sql.append(" ORDER BY o.created_at DESC LIMIT 200");

        List<Object[]> list = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[7];
                    for (int i = 0; i < 7; i++) row[i] = rs.getObject(i + 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load orders: " + e.getMessage(), e);
        }
        return list;
    }

    /** Orders for one cashier (by username, matched against the currently logged-in user). */
    public List<Object[]> getOpenOrdersByServer(int userId) {
        String sql = "SELECT o.id, o.order_source, o.order_type, COALESCE(c.name, 'Walk-in'), u.full_name, o.total, o.created_at " +
                     "FROM orders o JOIN users u ON o.user_id = u.id LEFT JOIN customers c ON o.customer_id = c.id " +
                     "WHERE o.status = 'OPEN' AND o.user_id = ? ORDER BY o.created_at DESC";
        List<Object[]> list = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[7];
                    for (int i = 0; i < 7; i++) row[i] = rs.getObject(i + 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load orders for server: " + e.getMessage(), e);
        }
        return list;
    }

    public void cancelOrder(int orderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel order: " + e.getMessage(), e);
        }
    }

    public void markCompleted(int orderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = 'COMPLETED' WHERE id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete order: " + e.getMessage(), e);
        }
    }

    /** Moves every line item from {@code fromOrderId} into {@code intoOrderId}, recomputes the
     *  destination's subtotal/tax/total from its (now combined) line items, and cancels the
     *  now-empty source order. Used by the "Merge Checks" function. */
    public void mergeOrders(int fromOrderId, int intoOrderId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE order_items SET order_id = ? WHERE order_id = ?")) {
                ps.setInt(1, intoOrderId);
                ps.setInt(2, fromOrderId);
                ps.executeUpdate();
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(quantity * unit_price), 0) FROM order_items WHERE order_id = ?")) {
                ps.setInt(1, intoOrderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) subtotal = rs.getBigDecimal(1);
                }
            }
            BigDecimal tax = subtotal.multiply(new BigDecimal("0.08875")).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(tax);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET subtotal = ?, tax = ?, total = ? WHERE id = ?")) {
                ps.setBigDecimal(1, subtotal);
                ps.setBigDecimal(2, tax);
                ps.setBigDecimal(3, total);
                ps.setInt(4, intoOrderId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE id = ?")) {
                ps.setInt(1, fromOrderId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Failed to merge checks: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /** Overwrites a completed order's discount/total (Adjust Closed Check). */
    public void adjustOrderTotals(int orderId, BigDecimal newDiscount, BigDecimal newTotal) {
        String sql = "UPDATE orders SET discount = ?, total = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newDiscount);
            ps.setBigDecimal(2, newTotal);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to adjust order: " + e.getMessage(), e);
        }
    }

    /** Records a manual credit or a return as an extra payments row (negative amount for a return). */
    public void insertManualPayment(int orderId, String method, BigDecimal amount, String reference) {
        String sql = "INSERT INTO payments (order_id, method, amount, status, reference_number) VALUES (?,?,?, 'SUCCESS', ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, method);
            ps.setBigDecimal(3, amount);
            ps.setString(4, reference);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record payment: " + e.getMessage(), e);
        }
    }

    /** [itemName, quantity, unitPrice, lineTotal] for every line on an order — used by
     *  Recall DT Order / Print OTG Ticket to show real order contents. */
    public List<Object[]> getOrderItems(int orderId) {
        String sql = "SELECT m.name, oi.quantity, oi.unit_price, (oi.quantity * oi.unit_price) " +
                     "FROM order_items oi JOIN menu_items m ON oi.menu_item_id = m.id WHERE oi.order_id = ?";
        List<Object[]> list = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[4];
                    for (int i = 0; i < 4; i++) row[i] = rs.getObject(i + 1);
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load order items: " + e.getMessage(), e);
        }
        return list;
    }

    public Object[] getOrderHeader(int orderId) {
        String sql = "SELECT o.id, o.order_source, o.order_type, COALESCE(c.name,'Walk-in'), u.full_name, o.total, o.status " +
                     "FROM orders o JOIN users u ON o.user_id = u.id LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object[] row = new Object[7];
                    for (int i = 0; i < 7; i++) row[i] = rs.getObject(i + 1);
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load order: " + e.getMessage(), e);
        }
        return null;
    }
}
