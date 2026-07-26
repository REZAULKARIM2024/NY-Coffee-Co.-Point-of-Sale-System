package com.possystem.model;

import java.math.BigDecimal;

public class Ingredient {
    private int id;
    private String name;
    private String unit;
    private BigDecimal stockQuantity;
    private BigDecimal lowStockThreshold;
    private BigDecimal unitCost;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(BigDecimal lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public boolean isLowStock() {
        return stockQuantity != null && lowStockThreshold != null
            && stockQuantity.compareTo(lowStockThreshold) <= 0;
    }

    @Override
    public String toString() { return name; }
}
