package com.possystem.model;

public class Category {
    private int id;
    private String name;
    private String station;
    private int sortOrder;
    private Integer parentId; // null = top-level department tab; set = subcategory under that department

    public Category() {}

    public Category(int id, String name, String station) {
        this.id = id;
        this.name = name;
        this.station = station;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public boolean isTopLevel() { return parentId == null; }

    @Override
    public String toString() { return name; }
}
