package com.possystem.api;

import com.possystem.dao.MenuItemDAO;
import com.possystem.dao.EmployeeDAO;
import com.possystem.dao.OrderDAO;
import com.possystem.model.CartItem;
import com.possystem.model.Employee;
import com.possystem.model.MenuItem;
import com.possystem.service.POSService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A small, dependency-free REST API layer on top of the existing DAO/service classes, using
 * only what's built into the JDK (com.sun.net.httpserver.HttpServer) — no Spring Boot, since
 * this sandbox has no route to Maven Central to pull one in. Exposes read endpoints for menu
 * items, employees, and orders, plus a POST /api/checkout that reuses POSService so the API
 * and the Swing UI share the exact same business logic and DB transaction path.
 *
 * Run with: java -cp "target/classes;lib/mysql-connector-j-9.7.0/...jar" com.possystem.api.ApiServer [port]
 * Default port: 8080.
 */
public class ApiServer {

    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final POSService posService = new POSService();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) { }
        }
        new ApiServer().start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/menu-items", this::handleMenuItems);
        server.createContext("/api/employees", this::handleEmployees);
        server.createContext("/api/orders", this::handleOrders);
        server.createContext("/api/checkout", this::handleCheckout);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("NY Coffee Co. API listening on http://localhost:" + port + "/api/health");
    }

    // ---------- Handlers ----------

    private void handleHealth(HttpExchange ex) throws IOException {
        if (!requireMethod(ex, "GET")) return;
        String dbStatus;
        try (java.sql.Connection c = com.possystem.config.DBConnection.getConnection()) {
            dbStatus = "up";
        } catch (SQLException e) {
            dbStatus = "down: " + e.getMessage();
        }
        Map<String, Object> body = Json.obj(
                "status", "ok",
                "service", "ny-coffee-pos-api",
                "database", dbStatus,
                "timestamp", java.time.Instant.now().toString());
        sendJson(ex, 200, body);
    }

    private void handleMenuItems(HttpExchange ex) throws IOException {
        if (!requireMethod(ex, "GET")) return;
        try {
            String path = ex.getRequestURI().getPath(); // /api/menu-items or /api/menu-items/{id}
            String[] parts = path.split("/");
            // parts: ["", "api", "menu-items", "{id}"?]
            if (parts.length >= 4 && !parts[3].isEmpty()) {
                int id = Integer.parseInt(parts[3]);
                MenuItem item = findMenuItemById(id);
                if (item == null) {
                    sendJson(ex, 404, Json.obj("error", "Menu item " + id + " not found"));
                    return;
                }
                sendJson(ex, 200, menuItemToJson(item));
                return;
            }

            String query = ex.getRequestURI().getQuery();
            Integer categoryId = queryParamInt(query, "category");
            List<MenuItem> items = categoryId != null
                    ? menuItemDAO.getMenuItemsByCategory(categoryId)
                    : menuItemDAO.getActiveMenuItems();

            List<Object> jsonItems = new ArrayList<>();
            for (MenuItem m : items) jsonItems.add(menuItemToJson(m));
            sendJson(ex, 200, Json.obj("count", jsonItems.size(), "items", jsonItems));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, Json.obj("error", "Invalid id/category — must be a number"));
        } catch (RuntimeException e) {
            sendJson(ex, 500, Json.obj("error", e.getMessage()));
        }
    }

    private void handleEmployees(HttpExchange ex) throws IOException {
        if (!requireMethod(ex, "GET")) return;
        try {
            List<Employee> employees = employeeDAO.getAllEmployees();
            List<Object> jsonEmployees = new ArrayList<>();
            for (Employee e : employees) {
                jsonEmployees.add(Json.obj(
                        "id", e.getId(),
                        "fullName", e.getFullName(),
                        "phone", e.getPhone(),
                        "email", e.getEmail(),
                        "position", e.getPosition(),
                        "hourlyRate", e.getHourlyRate() == null ? null : e.getHourlyRate().toPlainString(),
                        "active", e.isActive()));
            }
            sendJson(ex, 200, Json.obj("count", jsonEmployees.size(), "employees", jsonEmployees));
        } catch (RuntimeException e) {
            sendJson(ex, 500, Json.obj("error", e.getMessage()));
        }
    }

    private void handleOrders(HttpExchange ex) throws IOException {
        if (!requireMethod(ex, "GET")) return;
        try {
            String path = ex.getRequestURI().getPath(); // /api/orders/{id}
            String[] parts = path.split("/");
            if (parts.length < 4 || parts[3].isEmpty()) {
                sendJson(ex, 400, Json.obj("error", "Usage: GET /api/orders/{id}"));
                return;
            }
            int orderId = Integer.parseInt(parts[3]);
            Object[] header = orderDAO.getOrderHeader(orderId);
            if (header == null) {
                sendJson(ex, 404, Json.obj("error", "Order " + orderId + " not found"));
                return;
            }
            List<Object[]> rows = orderDAO.getOrderItems(orderId);
            List<Object> items = new ArrayList<>();
            for (Object[] row : rows) {
                items.add(Json.obj(
                        "itemName", row[0],
                        "quantity", row[1],
                        "unitPrice", ((BigDecimal) row[2]).toPlainString(),
                        "lineTotal", ((BigDecimal) row[3]).toPlainString()));
            }
            Map<String, Object> body = Json.obj(
                    "id", header[0],
                    "orderSource", header[1],
                    "orderType", header[2],
                    "customerName", header[3],
                    "cashierName", header[4],
                    "total", ((BigDecimal) header[5]).toPlainString(),
                    "status", header[6],
                    "items", items);
            sendJson(ex, 200, body);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, Json.obj("error", "Order id must be a number"));
        } catch (RuntimeException e) {
            sendJson(ex, 500, Json.obj("error", e.getMessage()));
        }
    }

    /**
     * POST /api/checkout
     * {
     *   "userId": 1, "customerId": null, "paymentMethod": "CASH",
     *   "orderSource": "ONLINE", "orderType": "PICKUP", "discount": 0,
     *   "items": [ { "menuItemId": 12, "quantity": 2 } ]
     * }
     */
    private void handleCheckout(HttpExchange ex) throws IOException {
        if (!requireMethod(ex, "POST")) return;
        try {
            String body = readBody(ex);
            Object parsed = Json.parse(body);
            if (!(parsed instanceof Map)) {
                sendJson(ex, 400, Json.obj("error", "Request body must be a JSON object"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> req = (Map<String, Object>) parsed;

            int userId = intField(req, "userId", -1);
            if (userId <= 0) {
                sendJson(ex, 400, Json.obj("error", "userId is required and must be a positive integer"));
                return;
            }
            Integer customerId = req.get("customerId") == null ? null : intField(req, "customerId", -1);
            String paymentMethod = stringField(req, "paymentMethod", "CASH");
            String orderSource = stringField(req, "orderSource", "ONLINE");
            String orderType = stringField(req, "orderType", "PICKUP");
            BigDecimal discount = decimalField(req, "discount", BigDecimal.ZERO);

            Object itemsObj = req.get("items");
            if (!(itemsObj instanceof List) || ((List<?>) itemsObj).isEmpty()) {
                sendJson(ex, 400, Json.obj("error", "items must be a non-empty array of {menuItemId, quantity}"));
                return;
            }

            List<CartItem> cart = new ArrayList<>();
            for (Object o : (List<?>) itemsObj) {
                if (!(o instanceof Map)) {
                    sendJson(ex, 400, Json.obj("error", "Each item must be an object with menuItemId and quantity"));
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> itemReq = (Map<String, Object>) o;
                int menuItemId = intField(itemReq, "menuItemId", -1);
                int quantity = intField(itemReq, "quantity", 1);
                if (menuItemId <= 0 || quantity <= 0) {
                    sendJson(ex, 400, Json.obj("error", "menuItemId and quantity must be positive integers"));
                    return;
                }
                MenuItem menuItem = findMenuItemById(menuItemId);
                if (menuItem == null) {
                    sendJson(ex, 404, Json.obj("error", "Menu item " + menuItemId + " not found or inactive"));
                    return;
                }
                cart.add(new CartItem(menuItem, quantity));
            }

            POSService.CheckoutResult result = posService.checkout(
                    cart, discount, userId, customerId, paymentMethod, orderSource, orderType);

            Map<String, Object> resp = Json.obj(
                    "orderId", result.orderId,
                    "subtotal", result.subtotal.toPlainString(),
                    "discount", (result.discount == null ? BigDecimal.ZERO : result.discount).toPlainString(),
                    "tax", result.tax.toPlainString(),
                    "total", result.total.toPlainString(),
                    "paymentReference", result.paymentReference);
            sendJson(ex, 201, resp);
        } catch (Json.JsonException e) {
            sendJson(ex, 400, Json.obj("error", "Malformed JSON: " + e.getMessage()));
        } catch (OrderDAO.InsufficientStockException e) {
            sendJson(ex, 409, Json.obj("error", e.getMessage()));
        } catch (RuntimeException e) {
            sendJson(ex, 500, Json.obj("error", e.getMessage()));
        }
    }

    // ---------- Helpers ----------

    private MenuItem findMenuItemById(int id) {
        for (MenuItem m : menuItemDAO.getAllMenuItems()) {
            if (m.getId() == id) return m;
        }
        return null;
    }

    private Map<String, Object> menuItemToJson(MenuItem m) {
        return Json.obj(
                "id", m.getId(),
                "categoryId", m.getCategoryId(),
                "categoryName", m.getCategoryName(),
                "name", m.getName(),
                "description", m.getDescription(),
                "price", m.getPrice() == null ? null : m.getPrice().toPlainString(),
                "section", m.getSection(),
                "active", m.isActive());
    }

    private boolean requireMethod(HttpExchange ex, String method) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase(method)) {
            sendJson(ex, 405, Json.obj("error", "Method not allowed — use " + method));
            return false;
        }
        return true;
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody(); ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
            return buf.toString(StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Integer queryParamInt(String query, String name) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                try { return Integer.parseInt(kv[1]); } catch (NumberFormatException e) { return null; }
            }
        }
        return null;
    }

    private int intField(Map<String, Object> map, String key, int defaultValue) {
        Object v = map.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private String stringField(Map<String, Object> map, String key, String defaultValue) {
        Object v = map.get(key);
        return v == null ? defaultValue : v.toString();
    }

    private BigDecimal decimalField(Map<String, Object> map, String key, BigDecimal defaultValue) {
        Object v = map.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return new BigDecimal(v.toString());
        try { return new BigDecimal(v.toString()); } catch (NumberFormatException e) { return defaultValue; }
    }
}
