package com.possystem.api.bdd;

import com.possystem.api.Json;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assume;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for the REST API Cucumber scenarios under src/test/resources/features/api.
 *
 * Follows the same "skip rather than fail" convention as
 * {@code com.possystem.api.ApiIntegrationTest}: if the API server isn't reachable at BASE_URL,
 * every scenario is skipped via {@link org.junit.Assume#assumeTrue(String, boolean)} — which
 * Cucumber recognizes as a SKIPPED result — instead of failing, so {@code mvn test} stays green
 * in environments without the app/DB running (e.g. a plain IDE run), while still executing for
 * real once run_api_server.bat (or the GitHub Actions CI job) has the server up.
 */
public class ApiSteps {

    static final String BASE_URL = "http://localhost:8081";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    private HttpResponse<String> lastResponse;
    private Map<String, Object> lastJson;
    private Long checkoutMenuItemId;
    private Long checkoutOrderId;

    @Before
    public void serverMustBeRunning() {
        boolean reachable;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            reachable = resp.statusCode() == 200;
        } catch (Exception e) {
            reachable = false;
        }
        Assume.assumeTrue(
                "API server not reachable at " + BASE_URL
                        + " — start it with run_api_server.bat first. Skipping API BDD scenarios.",
                reachable);
    }

    @Given("the API server is running")
    public void theApiServerIsRunning() {
        // Reachability is already verified in the @Before hook; this step exists for readability.
    }

    @When("I GET {string}")
    public void iGet(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET().build();
        lastResponse = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        lastJson = parseIfJson(lastResponse.body());
    }

    @When("I POST {string} with body:")
    public void iPostWithBody(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        lastResponse = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        lastJson = parseIfJson(lastResponse.body());
    }

    @Given("I look up the first active menu item")
    public void iLookUpTheFirstActiveMenuItem() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/menu-items")).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = parseIfJson(resp.body());
        List<?> items = (List<?>) body.get("items");
        Assume.assumeTrue("No active menu items seeded — skipping checkout scenario.",
                items != null && !items.isEmpty());
        Map<?, ?> first = (Map<?, ?>) items.get(0);
        checkoutMenuItemId = ((Number) first.get("id")).longValue();
    }

    @When("I checkout that menu item as user {int}")
    public void iCheckoutThatMenuItemAsUser(int userId) throws Exception {
        String payload = String.format(
                "{\"userId\":%d,\"paymentMethod\":\"CASH\",\"orderSource\":\"ONLINE\",\"orderType\":\"PICKUP\"," +
                        "\"items\":[{\"menuItemId\":%d,\"quantity\":1}]}",
                userId, checkoutMenuItemId);
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/checkout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        lastResponse = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        lastJson = parseIfJson(lastResponse.body());
        if (lastResponse.statusCode() == 201 && lastJson.get("orderId") != null) {
            checkoutOrderId = ((Number) lastJson.get("orderId")).longValue();
        }
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expected) {
        assertEquals(expected, lastResponse.statusCode(), "Unexpected status. Body was: " + lastResponse.body());
    }

    @Then("the response field {string} should equal {string}")
    public void theResponseFieldShouldEqual(String field, String expected) {
        assertEquals(expected, String.valueOf(lastJson.get(field)));
    }

    @Then("the response field {string} should be a list")
    public void theResponseFieldShouldBeAList(String field) {
        assertTrue(lastJson.get(field) instanceof List, field + " was not a list: " + lastJson.get(field));
    }

    @Then("I can fetch the resulting order and it contains at least {int} item")
    public void iCanFetchTheResultingOrderAndItContainsAtLeastItem(int minItems) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/orders/" + checkoutOrderId)).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        Map<String, Object> order = parseIfJson(resp.body());
        List<?> items = (List<?>) order.get("items");
        assertTrue(items != null && items.size() >= minItems);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseIfJson(String body) {
        try {
            Object parsed = Json.parse(body);
            return parsed instanceof Map ? (Map<String, Object>) parsed : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }
}
