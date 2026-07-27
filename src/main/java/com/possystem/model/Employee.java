package com.possystem.model;

import java.math.BigDecimal;

public class Employee {
    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String position;
    private BigDecimal hourlyRate;
    private boolean active;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return fullName; }
}
