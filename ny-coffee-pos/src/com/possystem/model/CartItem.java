package com.possystem.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartItem {
    private MenuItem menuItem;
    private int quantity;
    private boolean loyaltyFree;
    private Size size;                       // carries its own price_delta
    private String temperature;              // Hot / Iced (no price effect)
    private List<Modifier> modifiers = new ArrayList<>(); // each carries its own price_delta

    public CartItem(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public Size getSize() { return size; }
    public void setSize(Size size) { this.size = size; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public List<Modifier> getModifiers() { return modifiers; }
    public void addModifier(Modifier modifier) { modifiers.add(modifier); }
    public void toggleModifier(Modifier modifier) {
        if (!modifiers.remove(modifier)) modifiers.add(modifier);
    }
    public boolean hasModifier(Modifier modifier) { return modifiers.contains(modifier); }

    /** Multi-line display text matching a receipt-style cart line. */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (temperature != null) sb.append(temperature).append(" ");
        sb.append(menuItem.getName());
        return sb.toString();
    }

    public String getModifierSummary() {
        StringBuilder sb = new StringBuilder();
        if (size != null) sb.append(size.getName()).append(" ");
        if (!modifiers.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Modifier m : modifiers) names.add(m.getName());
            sb.append(String.join(", ", names));
        }
        return sb.toString().trim();
    }

    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isLoyaltyFree() { return loyaltyFree; }
    public void setLoyaltyFree(boolean loyaltyFree) { this.loyaltyFree = loyaltyFree; }

    /** Base menu price + size upcharge + every selected modifier's upcharge. */
    public BigDecimal getUnitPrice() {
        BigDecimal price = menuItem.getPrice();
        if (size != null && size.getPriceDelta() != null) price = price.add(size.getPriceDelta());
        for (Modifier m : modifiers) {
            if (m.getPriceDelta() != null) price = price.add(m.getPriceDelta());
        }
        return price;
    }

    public BigDecimal getLineTotal() {
        if (loyaltyFree) return BigDecimal.ZERO;
        return getUnitPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
