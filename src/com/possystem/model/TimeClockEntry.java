package com.possystem.model;

import java.sql.Timestamp;

public class TimeClockEntry {
    private int id;
    private int employeeId;
    private String employeeName; // populated on joined reads, otherwise null
    private Timestamp clockIn;
    private Timestamp clockOut; // null while the employee is still clocked in

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Timestamp getClockIn() { return clockIn; }
    public void setClockIn(Timestamp clockIn) { this.clockIn = clockIn; }

    public Timestamp getClockOut() { return clockOut; }
    public void setClockOut(Timestamp clockOut) { this.clockOut = clockOut; }

    public boolean isOpen() { return clockOut == null; }
}
