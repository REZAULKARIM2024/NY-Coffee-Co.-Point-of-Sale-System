package com.possystem.model;

import java.math.BigDecimal;

public class Modifier {
    private int id;
    private String name;
    private BigDecimal priceDelta;
    private String group;

    public Modifier() {}

    public Modifier(int id, String name, BigDecimal priceDelta, String group) {
        this.id = id;
        this.name = name;
        this.priceDelta = priceDelta;
        this.group = group;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    @Override
    public String toString() {
        return priceDelta.compareTo(BigDecimal.ZERO) > 0 ? name + " (+$" + priceDelta + ")" : name;
    }
}
