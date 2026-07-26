package com.possystem.model;

import java.math.BigDecimal;

public class Size {
    private int id;
    private String name;
    private BigDecimal priceDelta;

    public Size() {}

    public Size(int id, String name, BigDecimal priceDelta) {
        this.id = id;
        this.name = name;
        this.priceDelta = priceDelta;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }

    @Override
    public String toString() { return name; }
}
