package com.possystem.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for CartItem's price math: base price + size delta + modifier deltas,
 * multiplied by quantity, with loyalty-free items always totaling zero.
 */
@Tag("unit")
class CartItemTest {

    private MenuItem menuItem(String price) {
        MenuItem m = new MenuItem();
        m.setId(1);
        m.setName("Latte");
        m.setPrice(new BigDecimal(price));
        m.setActive(true);
        return m;
    }

    @Test
    void getUnitPrice_noSizeOrModifiers_returnsBasePrice() {
        CartItem item = new CartItem(menuItem("4.50"), 1);
        assertEquals(new BigDecimal("4.50"), item.getUnitPrice());
    }

    @Test
    void getUnitPrice_withSize_addsPriceDelta() {
        CartItem item = new CartItem(menuItem("4.50"), 1);
        item.setSize(new Size(1, "Large", new BigDecimal("1.00")));
        assertEquals(new BigDecimal("5.50"), item.getUnitPrice());
    }

    @Test
    void getUnitPrice_withModifiers_sumsEachDelta() {
        CartItem item = new CartItem(menuItem("4.50"), 1);
        item.addModifier(new Modifier(1, "Extra Shot", new BigDecimal("0.75"), "Add-ons"));
        item.addModifier(new Modifier(2, "Oat Milk", new BigDecimal("0.60"), "Dairy"));
        // 4.50 + 0.75 + 0.60 = 5.85
        assertEquals(new BigDecimal("5.85"), item.getUnitPrice());
    }

    @Test
    void getUnitPrice_sizeAndModifiersCombined() {
        CartItem item = new CartItem(menuItem("4.50"), 1);
        item.setSize(new Size(1, "Large", new BigDecimal("1.00")));
        item.addModifier(new Modifier(1, "Extra Shot", new BigDecimal("0.75"), "Add-ons"));
        // 4.50 + 1.00 + 0.75 = 6.25
        assertEquals(new BigDecimal("6.25"), item.getUnitPrice());
    }

    @Test
    void getLineTotal_multipliesUnitPriceByQuantity() {
        CartItem item = new CartItem(menuItem("4.50"), 3);
        assertEquals(new BigDecimal("13.50"), item.getLineTotal());
    }

    @Test
    void getLineTotal_loyaltyFree_isAlwaysZero() {
        CartItem item = new CartItem(menuItem("4.50"), 3);
        item.setSize(new Size(1, "Large", new BigDecimal("1.00")));
        item.setLoyaltyFree(true);
        assertEquals(BigDecimal.ZERO, item.getLineTotal());
    }

    @Test
    void toggleModifier_addsThenRemoves() {
        CartItem item = new CartItem(menuItem("4.50"), 1);
        Modifier extraShot = new Modifier(1, "Extra Shot", new BigDecimal("0.75"), "Add-ons");

        item.toggleModifier(extraShot);
        assertEquals(true, item.hasModifier(extraShot));
        assertEquals(new BigDecimal("5.25"), item.getUnitPrice());

        item.toggleModifier(extraShot);
        assertEquals(false, item.hasModifier(extraShot));
        assertEquals(new BigDecimal("4.50"), item.getUnitPrice());
    }
}
