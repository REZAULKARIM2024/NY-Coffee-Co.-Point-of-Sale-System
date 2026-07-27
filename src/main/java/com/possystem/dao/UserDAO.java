package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.User;
import com.possystem.util.PasswordUtil;

import java.sql.*;

public class UserDAO {

    /** Returns the User if username/password match an active account, else null. */
    public User authenticate(String username, String plainPassword) {
        String sql = "SELECT * FROM users WHERE username = ? AND active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String salt = rs.getString("salt");
                    String hash = rs.getString("password_hash");
                    if (PasswordUtil.verify(plainPassword, salt, hash)) {
                        User u = new User();
                        u.setId(rs.getInt("id"));
                        u.setUsername(rs.getString("username"));
                        u.setFullName(rs.getString("full_name"));
                        u.setRole(rs.getString("role"));
                        u.setActive(rs.getBoolean("active"));
                        int empId = rs.getInt("employee_id");
                        u.setEmployeeId(rs.wasNull() ? null : empId);
                        return u;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Login query failed: " + e.getMessage(), e);
        }
        return null;
    }

    public void createUser(String username, String plainPassword, String fullName, String role, Integer employeeId) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(plainPassword, salt);
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, role, employee_id) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, fullName);
            ps.setString(5, role);
            if (employeeId != null) ps.setInt(6, employeeId); else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }
}
