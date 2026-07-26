package com.possystem.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live HTTP integration tests against com.possystem.api.ApiServer, using only java.net.http
 * (built into the JDK — no extra HTTP client dependency needed).
 *
 * These are NOT run as part of the normal offline unit-test suite: they require the API server
 * to actually be running with a live MySQL connection (run_api_server.bat) at http://localhost:8080.
 * If the server isn't reachable, every test in this class is skipped (via Assumptions) rather
 * than failed, so run_tests.bat stays green in environments without the app/DB running.
 *
 * To exercise this class for real: start `run_api_server.bat`, seed at least one active menu
 * item and one user, then run:
 *   java -cp "target/classes;target/test-classes;lib/junit5/*" org.junit.platform.console.ConsoleLauncher --select-class com.possystem.api.ApiIntegrationTest
 */
class ApiIntegrationTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @BeforeAll
    static void checkServerIsRunning() {
        boolean reachable;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            reachable = resp.statusCode() == 200;
        } catch (Exception e) {
            reachable = false;
        }
        assumeTrue(reachable, "API server not reachable at " + BASE_URL + " — start it with run_api_server.bat first. Skipping live integration tests.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJson(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        return (Map<String, Object>) Json.parse(resp.body());
    }

    @Test
    void health_returnsOkStatus() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/health")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        Map<String, Object> body = (Map<String, Object>) Json.parse(resp.body());
        assertEquals("ok", body.get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void menuItems_returnsArray() throws Exception {
        Map<String, Object> body = getJson("/api/menu-items");
        assertTrue(body.get("items") instanceof List);
    }

    @Test
    void checkout_missingUserId_returns400() throws Exception {
        String payload = "{\"paymentMethod\":\"CASH\",\"items\":[{\"menuItemId\":1,\"quantity\":1}]}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/checkout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkout_thenFetchOrder_roundTrips() throws Exception {
        Map<String, Object> menu = getJson("/api/menu-items");
        List<Object> items = (List<Object>) menu.get("items");
        assumeTrue(!items.isEmpty(), "No active menu items seeded — skipping checkout round-trip test.");
        Map<String, Object> firstItem = (Map<String, Object>) items.get(0);
        long menuItemId = (Long) firstItem.get("id");

        String payload = String.format(
                "{\"userId\":1,\"paymentMethod\":\"CASH\",\"orderSource\":\"ONLINE\",\"orderType\":\"PICKUP\"," +
                "\"items\":[{\"menuItemId\":%d,\"quantity\":1}]}", menuItemId);
        HttpRequest checkoutReq = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/checkout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> checkoutResp = client.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, checkoutResp.statusCode());

        Map<String, Object> checkoutBody = (Map<String, Object>) Json.parse(checkoutResp.body());
        long orderId = (Long) checkoutBody.get("orderId");

        Map<String, Object> order = getJson("/api/orders/" + orderId);
        assertEquals(orderId, ((Number) order.get("id")).longValue());
        assertTrue(order.get("items") instanceof List);
        assertTrue(((List<Object>) order.get("items")).size() >= 1);
    }
}
