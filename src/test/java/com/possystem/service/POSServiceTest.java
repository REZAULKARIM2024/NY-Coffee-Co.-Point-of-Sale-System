package com.possystem.service;

import com.possystem.model.CartItem;
import com.possystem.model.MenuItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the pure calculation logic in POSService (subtotal/tax math). These do not
 * touch the database — POSService.calculateSubtotal/calculateTax operate only on in-memory
 * CartItem objects, so they're safe to test in isolation.
 */
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
}
