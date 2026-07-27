package com.possystem.gui;

import com.possystem.dao.EmployeeDAO;
import com.possystem.dao.UserDAO;
import com.possystem.model.Employee;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class EmployeePanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final UserDAO userDAO = new UserDAO();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"ID", "Name", "Position", "Phone", "Email", "Hourly Rate", "Active"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Employee> loaded;

    public EmployeePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        JTable table = new JTable(tableModel);
        UITheme.styleTable(table, UITheme.NAV_EMPLOYEES);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = UITheme.styledButton("Add Employee", UITheme.NAV_EMPLOYEES);
        JButton editBtn = UITheme.styledButton("Edit Selected", UITheme.NAV_MENU_MGMT);
        JButton loginBtn = UITheme.styledButton("Create Login for Selected", UITheme.NAV_RECIPES);

        addBtn.addActionListener(e -> openEditor(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) openEditor(loaded.get(row));
        });
        loginBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) createLogin(loaded.get(row));
            else JOptionPane.showMessageDialog(this, "Select an employee first.");
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(loginBtn);
        add(buttons, BorderLayout.SOUTH);

        loadTable();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            loaded = employeeDAO.getAllEmployees();
            for (Employee e : loaded) {
                tableModel.addRow(new Object[]{
                    e.getId(), e.getFullName(), e.getPosition(), e.getPhone(), e.getEmail(), e.getHourlyRate(), e.isActive()
                });
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load employees: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEditor(Employee existing) {
        JTextField nameField = new JTextField(existing != null ? existing.getFullName() : "");
        JTextField positionField = new JTextField(existing != null ? existing.getPosition() : "");
        JTextField phoneField = new JTextField(existing != null ? existing.getPhone() : "");
        JTextField emailField = new JTextField(existing != null ? existing.getEmail() : "");
        JTextField rateField = new JTextField(existing != null && existing.getHourlyRate() != null ? existing.getHourlyRate().toString() : "0.00");
        JCheckBox activeBox = new JCheckBox("Active", existing == null || existing.isActive());

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Full Name:")); form.add(nameField);
        form.add(new JLabel("Position:")); form.add(positionField);
        form.add(new JLabel("Phone:")); form.add(phoneField);
        form.add(new JLabel("Email:")); form.add(emailField);
        form.add(new JLabel("Hourly Rate:")); form.add(rateField);
        form.add(new JLabel("")); form.add(activeBox);

        int result = JOptionPane.showConfirmDialog(this, form,
            existing == null ? "Add Employee" : "Edit Employee",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("[0-9+()\\-\\s]{7,20}")) {
            JOptionPane.showMessageDialog(this, "Enter a valid phone number or leave it blank.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            JOptionPane.showMessageDialog(this, "Enter a valid email or leave it blank.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        BigDecimal rate;
        try {
            rate = new BigDecimal(rateField.getText().trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Hourly rate must be a non-negative number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee emp = existing != null ? existing : new Employee();
        emp.setFullName(nameField.getText().trim());
        emp.setPosition(positionField.getText().trim());
        emp.setPhone(phone);
        emp.setEmail(email);
        emp.setHourlyRate(rate);
        emp.setActive(activeBox.isSelected());

        try {
            employeeDAO.saveEmployee(emp);
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createLogin(Employee emp) {
        JTextField usernameField = new JTextField(emp.getFullName().toLowerCase().replaceAll("\\s+", "."));
        JPasswordField passwordField = new JPasswordField();
        String[] roles = {"CASHIER", "MANAGER", "ADMIN"};
        JComboBox<String> roleBox = new JComboBox<>(roles);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Username:")); form.add(usernameField);
        form.add(new JLabel("Temporary Password:")); form.add(passwordField);
        form.add(new JLabel("Role:")); form.add(roleBox);

        int result = JOptionPane.showConfirmDialog(this, form, "Create Login for " + emp.getFullName(),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Username is required and password must be at least 6 characters.",
                "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            userDAO.createUser(username, password, emp.getFullName(), (String) roleBox.getSelectedItem(), emp.getId());
            JOptionPane.showMessageDialog(this, "Login created for " + emp.getFullName() + ".");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to create login: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
