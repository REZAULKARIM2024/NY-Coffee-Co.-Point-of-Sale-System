package com.possystem.service;

import com.possystem.model.CartItem;
import com.possystem.model.MenuItem;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the pure calculation logic in POSService (subtotal/tax math). These do not
 * touch the database — POSService.calculateSubtotal/calculateTax operate only on in-memory
 * CartItem objects, so they're safe to test in isolation.
 */
@Tag("unit")
class POSServiceTest {

    private final POSService posService = new POSService();

    private MenuItem menuItem(String name, String price) {
        MenuItem m = new MenuItem();
        m.setId(1);
        m.setName(name);
        m.setPrice(new BigDecimal(price));
        m.setActive(true);
        return m;
    }

    @Test
    void calculateSubtotal_emptyCart_isZero() {
        BigDecimal subtotal = posService.calculateSubtotal(new ArrayList<>());
        assertEquals(new BigDecimal("0.00"), subtotal);
    }

    @Test
    void calculateSubtotal_singleItem_multipliesByQuantity() {
        List<CartItem> cart = new ArrayList<>();
        cart.add(new CartItem(menuItem("Cold Brew", "3.75"), 2));
        BigDecimal subtotal = posService.calculateSubtotal(cart);
        assertEquals(new BigDecimal("7.50"), subtotal);
    }

    @Test
    void calculateSubtotal_multipleItems_sumsLineTotals() {
        List<CartItem> cart = new ArrayList<>();
        cart.add(new CartItem(menuItem("Cold Brew", "3.75"), 1));
        cart.add(new CartItem(menuItem("Bagel", "2.50"), 3));
        BigDecimal subtotal = posService.calculateSubtotal(cart);
        // 3.75 + (2.50 * 3) = 3.75 + 7.50 = 11.25
        assertEquals(new BigDecimal("11.25"), subtotal);
    }

    @Test
    void calculateSubtotal_loyaltyFreeItem_contributesZero() {
        List<CartItem> cart = new ArrayList<>();
        CartItem free = new CartItem(menuItem("Cold Brew", "3.75"), 1);
        free.setLoyaltyFree(true);
        cart.add(free);
        cart.add(new CartItem(menuItem("Bagel", "2.50"), 1));
        BigDecimal subtotal = posService.calculateSubtotal(cart);
        assertEquals(new BigDecimal("2.50"), subtotal);
    }

    @Test
    void calculateTax_appliesConfiguredRate() {
        // POSService.TAX_RATE is 0.08 (8%)
        BigDecimal tax = posService.calculateTax(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("0.80"), tax);
    }

    @Test
    void calculateTax_roundsToNearestCent() {
        // 4.05 * 0.08 = 0.324 -> rounds to 0.32
        BigDecimal tax = posService.calculateTax(new BigDecimal("4.05"));
        assertEquals(new BigDecimal("0.32"), tax);
    }

    @Test
    void calculateTax_zeroSubtotal_isZero() {
        BigDecimal tax = posService.calculateTax(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), tax);
    }

    // ---------- Data-driven / boundary cases ----------

    @ParameterizedTest(name = "subtotal={0} -> tax={1}")
    @CsvSource({
            "0.00, 0.00",
            "1.00, 0.08",
            "6.25, 0.50",
            "100.00, 8.00",
            "0.01, 0.00",      // 0.01 * 0.08 = 0.0008 -> rounds down to 0.00
            "12.345, 0.99"     // 12.345 * 0.08 = 0.9876 -> rounds up to 0.99
    })
    void calculateTax_variousAmounts_roundsHalfUpToNearestCent(String subtotal, String expectedTax) {
        BigDecimal tax = posService.calculateTax(new BigDecimal(subtotal));
        assertEquals(new BigDecimal(expectedTax), tax);
    }

    @Test
    void calculateTax_exactlyHalfwayBetweenCents_roundsHalfUpNotHalfEven() {
        // 1.5625 * 0.08 = 0.125 exactly. HALF_UP -> 0.13; HALF_EVEN would give 0.12.
        // This test exists specifically to pin down which rounding mode POSService uses.
        BigDecimal tax = posService.calculateTax(new BigDecimal("1.5625"));
        assertEquals(new BigDecimal("0.13"), tax);
    }

    @ParameterizedTest(name = "{0} x {1} -> {2}")
    @CsvSource({
            "3.75, 1, 3.75",
            "3.75, 2, 7.50",
            "2.50, 3, 7.50",
            "0.99, 5, 4.95",
            "10.00, 0, 0.00"   // zero-quantity edge case
    })
    void calculateSubtotal_variousPriceQuantityCombinations(String price, int quantity, String expectedSubtotal) {
        List<CartItem> cart = new ArrayList<>();
        cart.add(new CartItem(menuItem("Item", price), quantity));
        BigDecimal subtotal = posService.calculateSubtotal(cart);
        assertEquals(new BigDecimal(expectedSubtotal), subtotal);
    }
}
