package com.possystem.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String role; // ADMIN, MANAGER, CASHIER
    private boolean active;
    private Integer employeeId; // links this login to an employees row; null if not linked

    public User() {}

    public User(int id, String username, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.active = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isManagerOrAbove() { return "ADMIN".equals(role) || "MANAGER".equals(role); }
}
