package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.Employee;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees ORDER BY full_name";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load employees: " + e.getMessage(), e);
        }
        return list;
    }

    public void saveEmployee(Employee emp) {
        if (emp.getId() == 0) {
            String sql = "INSERT INTO employees (full_name, phone, email, position, hourly_rate, active) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, emp);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) emp.setId(keys.getInt(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to insert employee: " + e.getMessage(), e);
            }
        } else {
            String sql = "UPDATE employees SET full_name=?, phone=?, email=?, position=?, hourly_rate=?, active=? WHERE id=?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, emp);
                ps.setInt(7, emp.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update employee: " + e.getMessage(), e);
            }
        }
    }

    private void bind(PreparedStatement ps, Employee emp) throws SQLException {
        ps.setString(1, emp.getFullName());
        ps.setString(2, emp.getPhone());
        ps.setString(3, emp.getEmail());
        ps.setString(4, emp.getPosition());
        ps.setBigDecimal(5, emp.getHourlyRate() == null ? BigDecimal.ZERO : emp.getHourlyRate());
        ps.setBoolean(6, emp.isActive());
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("id"));
        e.setFullName(rs.getString("full_name"));
        e.setPhone(rs.getString("phone"));
        e.setEmail(rs.getString("email"));
        e.setPosition(rs.getString("position"));
        e.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        e.setActive(rs.getBoolean("active"));
        return e;
    }
}
