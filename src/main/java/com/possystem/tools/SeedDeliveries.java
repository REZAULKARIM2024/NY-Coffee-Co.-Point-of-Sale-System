package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One-off seeding utility: creates realistic delivery customers, DELIVERY-type orders
 * (with order_items + payments), and their matching deliveries rows spanning the full
 * status range (UNASSIGNED / ASSIGNED / PICKED_UP / DELIVERED), so the Delivery Queue
 * screen shows a believable live queue plus completed history.
 *
 * Usage: java -cp target/classes;lib/mysql-connector-j-*.jar com.possystem.tools.SeedDeliveries
 */
public class SeedDeliveries {

    private static final Random RND = new Random(2026);

    private static final String[] FIRST_NAMES = {
        "James","Maria","David","Linda","Michael","Sarah","Robert","Jennifer","William","Jessica",
        "Carlos","Fatima","Wei","Priya","Ahmed","Olga","Sofia","Marcus","Elena","Daniel",
        "Grace","Kevin","Nadia","Omar","Rachel","Tyler","Isabella","Noah","Aaliyah","Jack",
        "Mei","Diego","Amara","Liam","Chloe","Anton","Yuki","Leila","Victor","Camila"
    };
    private static final String[] LAST_NAMES = {
        "Smith","Johnson","Garcia","Rodriguez","Lee","Kim","Chen","Patel","Nguyen","Brown",
        "Davis","Martinez","Wilson","Cohen","O'Sullivan","Murphy","Santos","Ivanov","Ali","Khan",
        "Rossi","Dubois","Schmidt","Andersen","Kowalski","Silva","Costa","Hernandez","Torres","Ramirez"
    };
    private static final String[] STREETS = {
        "West 23rd St","East 14th St","Bedford Ave","5th Ave","Amsterdam Ave","Broadway",
        "Court St","Metropolitan Ave","Greenwich St","Lexington Ave","2nd Ave","7th Ave",
        "Myrtle Ave","DeKalb Ave","Columbus Ave","Hudson St","Prince St","Ludlow St",
        "Vernon Blvd","Northern Blvd","Roosevelt Ave","Grand St","Nassau Ave","Flatbush Ave"
    };
    private static final String[] NEIGHBORHOODS = {
        "New York, NY","Brooklyn, NY","Queens, NY","Astoria, NY","Long Island City, NY",
        "Williamsburg, NY","Park Slope, NY","Chelsea, NY","Greenwich Village, NY","Harlem, NY"
    };
    private static final String[] EMAIL_DOMAINS = {"gmail.com","yahoo.com","outlook.com","icloud.com"};
    private static final String[] AREA_CODES = {"212","718","347","917","646"};
    private static final String[] FALLBACK_DRIVERS = {"Marcus Webb", "Dana Reyes", "Tyrell Jackson"};

    public static void main(String[] args) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            int userId = getAdminUserId(conn);
            System.out.println("Using user_id=" + userId + " for order.user_id");

            List<int[]> menuItems = getMenuItemsWithPriceCents(conn); // [id, priceCents]
            System.out.println("Menu items available: " + menuItems.size());

            List<String> drivers = getDriverNames(conn);
            System.out.println("Delivery drivers found: " + drivers);

            List<Integer> customerIds = createCustomers(conn, 40);
            System.out.println("Customers created: " + customerIds.size());

            int orderCount = createOrdersAndDeliveries(conn, userId, menuItems, drivers, customerIds);
            System.out.println("Delivery orders created: " + orderCount);

            System.out.println("DONE.");
        }
    }

    private static int getAdminUserId(Connection conn) throws Exception {
        String sql = "SELECT id FROM users WHERE role='ADMIN' ORDER BY id LIMIT 1";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id FROM users ORDER BY id LIMIT 1")) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new RuntimeException("No users found in users table; cannot satisfy orders.user_id NOT NULL FK.");
    }

    private static List<int[]> getMenuItemsWithPriceCents(Connection conn) throws Exception {
        List<int[]> list = new ArrayList<>();
        String sql = "SELECT id, price FROM menu_items WHERE active = 1";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                int priceCents = rs.getBigDecimal("price").multiply(BigDecimal.valueOf(100)).intValue();
                list.add(new int[]{id, priceCents});
            }
        }
        return list;
    }

    private static List<String> getDriverNames(Connection conn) throws Exception {
        List<String> names = new ArrayList<>();
        String sql = "SELECT full_name FROM employees WHERE position LIKE '%Driver%' OR position LIKE '%Delivery%' AND active = 1";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) names.add(rs.getString(1));
        }
        if (names.isEmpty()) {
            for (String n : FALLBACK_DRIVERS) names.add(n);
        }
        return names;
    }

    private static List<Integer> createCustomers(Connection conn, int count) throws Exception {
        List<Integer> ids = new ArrayList<>();
        String checkSql = "SELECT 1 FROM customers WHERE phone = ?";
        String insertSql = "INSERT INTO customers (name, phone, email, loyalty_points) VALUES (?, ?, ?, ?)";
        try (PreparedStatement check = conn.prepareStatement(checkSql);
             PreparedStatement ins = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < count; i++) {
                String first = FIRST_NAMES[RND.nextInt(FIRST_NAMES.length)];
                String last = LAST_NAMES[RND.nextInt(LAST_NAMES.length)];
                String name = first + " " + last;

                String phone;
                while (true) {
                    String area = AREA_CODES[RND.nextInt(AREA_CODES.length)];
                    String phoneNum = String.format("(%s) 555-%04d", area, 1000 + RND.nextInt(9000));
                    check.setString(1, phoneNum);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) { phone = phoneNum; break; }
                    }
                }

                String emailLocal = (first + "." + last).toLowerCase().replace("'", "").replace(" ", "");
                String email = emailLocal + RND.nextInt(100) + "@" + EMAIL_DOMAINS[RND.nextInt(EMAIL_DOMAINS.length)];
                int loyalty = RND.nextInt(400);

                ins.setString(1, name);
                ins.setString(2, phone);
                ins.setString(3, email);
                ins.setInt(4, loyalty);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) ids.add(keys.getInt(1));
                }
            }
        }
        return ids;
    }

    private static String randomAddress() {
        int num = 10 + RND.nextInt(490);
        String street = STREETS[RND.nextInt(STREETS.length)];
        String apt = RND.nextBoolean() ? (", Apt " + (1 + RND.nextInt(12)) + (RND.nextBoolean() ? "" : "ABCD".charAt(RND.nextInt(4)) + "")) : "";
        String hood = NEIGHBORHOODS[RND.nextInt(NEIGHBORHOODS.length)];
        return num + " " + street + apt + ", " + hood;
    }

    private static int createOrdersAndDeliveries(Connection conn, int userId, List<int[]> menuItems,
                                                  List<String> drivers, List<Integer> customerIds) throws Exception {
        if (menuItems.isEmpty()) {
            throw new RuntimeException("No active menu_items found; cannot create order_items.");
        }

        String insertOrder = "INSERT INTO orders (order_source, order_type, customer_id, user_id, subtotal, discount, tax, total, status, created_at) " +
                "VALUES (?, 'DELIVERY', ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price, station_status) VALUES (?, ?, ?, ?, 'SERVED')";
        String insertPayment = "INSERT INTO payments (order_id, method, amount, status, reference_number, created_at) VALUES (?, ?, ?, 'SUCCESS', ?, ?)";
        String insertDelivery = "INSERT INTO deliveries (order_id, customer_address, assigned_to, status, created_at) VALUES (?, ?, ?, ?, ?)";

        String[] orderSources = {"ONLINE", "ONLINE", "ONLINE", "PHONE"};
        String[] payMethods = {"CARD", "CARD", "CARD", "MOBILE_BANKING", "CASH"};

        int totalOrders = 70;
        int created = 0;

        try (PreparedStatement psOrder = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psItem = conn.prepareStatement(insertItem);
             PreparedStatement psPay = conn.prepareStatement(insertPayment);
             PreparedStatement psDeliv = conn.prepareStatement(insertDelivery)) {

            for (int i = 0; i < totalOrders; i++) {
                // Spread orders across the last 21 days; the most recent ~3 days feed the "live" queue.
                boolean isRecent = i < 14; // first 14 -> recent/active queue
                int daysAgo = isRecent ? RND.nextInt(3) : 3 + RND.nextInt(19);
                int hour = 10 + RND.nextInt(11); // 10am - 9pm
                int minute = RND.nextInt(60);
                LocalDateTime orderTime = LocalDateTime.now().minusDays(daysAgo)
                        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);

                // Build items
                int itemCount = 2 + RND.nextInt(3); // 2-4 line items
                int subtotalCents = 0;
                List<int[]> chosen = new ArrayList<>(); // [menuItemId, qty, unitPriceCents]
                for (int k = 0; k < itemCount; k++) {
                    int[] mi = menuItems.get(RND.nextInt(menuItems.size()));
                    int qty = 1 + RND.nextInt(3);
                    chosen.add(new int[]{mi[0], qty, mi[1]});
                    subtotalCents += mi[1] * qty;
                }

                int discountCents = RND.nextDouble() < 0.15 ? (int) Math.round(subtotalCents * 0.10) : 0;
                int taxCents = (int) Math.round((subtotalCents - discountCents) * 0.08875);
                int totalCents = subtotalCents - discountCents + taxCents;

                BigDecimal subtotal = cents(subtotalCents);
                BigDecimal discount = cents(discountCents);
                BigDecimal tax = cents(taxCents);
                BigDecimal total = cents(totalCents);

                // Order status: recent ones mostly OPEN (still in flight), older ones COMPLETED.
                String orderStatus;
                if (isRecent) {
                    orderStatus = RND.nextDouble() < 0.75 ? "OPEN" : "COMPLETED";
                } else {
                    orderStatus = RND.nextDouble() < 0.95 ? "COMPLETED" : "CANCELLED";
                }

                int customerId = customerIds.get(RND.nextInt(customerIds.size()));
                String source = orderSources[RND.nextInt(orderSources.length)];

                psOrder.setString(1, source);
                psOrder.setInt(2, customerId);
                psOrder.setInt(3, userId);
                psOrder.setBigDecimal(4, subtotal);
                psOrder.setBigDecimal(5, discount);
                psOrder.setBigDecimal(6, tax);
                psOrder.setBigDecimal(7, total);
                psOrder.setString(8, orderStatus);
                psOrder.setTimestamp(9, Timestamp.valueOf(orderTime));
                psOrder.executeUpdate();

                int orderId;
                try (ResultSet keys = psOrder.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }

                for (int[] c : chosen) {
                    psItem.setInt(1, orderId);
                    psItem.setInt(2, c[0]);
                    psItem.setInt(3, c[1]);
                    psItem.setBigDecimal(4, cents(c[2]));
                    psItem.addBatch();
                }
                psItem.executeBatch();

                // Payments: skip for cancelled orders; skip for a few of the very newest OPEN orders
                // (not-yet-charged), everything else is a successful payment.
                boolean skipPayment = "CANCELLED".equals(orderStatus) || (isRecent && daysAgo == 0 && RND.nextDouble() < 0.3);
                if (!skipPayment) {
                    String method = payMethods[RND.nextInt(payMethods.length)];
                    psPay.setInt(1, orderId);
                    psPay.setString(2, method);
                    psPay.setBigDecimal(3, total);
                    psPay.setString(4, method + "-DEL-" + orderId + "-" + (1000 + RND.nextInt(9000)));
                    psPay.setTimestamp(5, Timestamp.valueOf(orderTime.plusMinutes(1)));
                    psPay.executeUpdate();
                }

                // Skip delivery row entirely for cancelled orders (never went out).
                if ("CANCELLED".equals(orderStatus)) {
                    created++;
                    continue;
                }

                String deliveryStatus;
                String assignedTo = null;
                if (orderStatus.equals("COMPLETED")) {
                    if (isRecent) {
                        // recent + completed: fully delivered already
                        deliveryStatus = "DELIVERED";
                        assignedTo = drivers.get(RND.nextInt(drivers.size()));
                    } else {
                        deliveryStatus = "DELIVERED";
                        assignedTo = drivers.get(RND.nextInt(drivers.size()));
                    }
                } else {
                    // OPEN order still in flight -> active queue, spread across the earlier stages
                    double r = RND.nextDouble();
                    if (r < 0.35) {
                        deliveryStatus = "UNASSIGNED";
                    } else if (r < 0.65) {
                        deliveryStatus = "ASSIGNED";
                        assignedTo = drivers.get(RND.nextInt(drivers.size()));
                    } else {
                        deliveryStatus = "PICKED_UP";
                        assignedTo = drivers.get(RND.nextInt(drivers.size()));
                    }
                }

                LocalDateTime deliveryCreated = orderTime.plusMinutes(2 + RND.nextInt(5));
                psDeliv.setInt(1, orderId);
                psDeliv.setString(2, randomAddress());
                psDeliv.setString(3, assignedTo);
                psDeliv.setString(4, deliveryStatus);
                psDeliv.setTimestamp(5, Timestamp.valueOf(deliveryCreated));
                psDeliv.executeUpdate();

                created++;
            }
        }
        return created;
    }

    private static BigDecimal cents(int c) {
        return BigDecimal.valueOf(c, 2).setScale(2, RoundingMode.HALF_UP);
    }
}
