package com.possystem.model;

import java.math.BigDecimal;

public class MenuItem {
    private int id;
    private int categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal cost;
    private String imagePath;
    private String section; // groups items under a header inside a subcategory's item grid
    private boolean active;

    public MenuItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return name; }
}
