package com.possystem.web;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Selenium E2E test against the /admin dashboard (src/main/resources/webapp/index.html),
 * driven headlessly via WebDriverManager-managed Chrome.
 *
 * Same "skip rather than fail" convention as ApiIntegrationTest: if the API server isn't
 * reachable at BASE_URL, both tests in this class are skipped rather than failed, so `mvn test`
 * stays green without the app/DB running. Start it with run_api_server.bat (or let the GitHub
 * Actions CI job start it) to exercise this class for real.
 */
class AdminPanelUiTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static WebDriver driver;

    @BeforeAll
    static void setUp() {
        boolean reachable;
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            reachable = resp.statusCode() == 200;
        } catch (Exception e) {
            reachable = false;
        }
        assumeTrue(reachable, "API server not reachable at " + BASE_URL
                + " — start it with run_api_server.bat first. Skipping Selenium UI tests.");

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1280,900", "--no-sandbox");
        driver = new ChromeDriver(options);
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void adminPage_showsHealthyStatus() {
        driver.get(BASE_URL + "/admin");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement status = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("health-status")));
        wait.until(d -> "ok".equalsIgnoreCase(status.getText()));
        assertEquals("ok", status.getText().toLowerCase());
    }

    @Test
    void adminPage_listsMenuItems() {
        driver.get(BASE_URL + "/admin");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#menu-items-table tbody tr"), 0));
        List<WebElement> rows = driver.findElements(By.cssSelector("#menu-items-table tbody tr"));
        assertTrue(rows.size() > 0, "Expected at least one menu item row rendered via JS fetch");
    }
}
