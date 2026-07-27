package com.possystem.gui;

import com.possystem.dao.SupplierDAO;
import com.possystem.model.Supplier;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Manager/admin tool: maintain the list of suppliers (roasters, dairy, bakery, packaging,
 * etc.) used for purchasing and inventory restocking.
 */
public class SupplierPanel extends JPanel {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"ID", "Name", "Contact Person", "Phone", "Email", "Address"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Supplier> loaded;

    public SupplierPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        JTable table = new JTable(tableModel);
        UITheme.styleTable(table, UITheme.NAV_SUPPLIERS);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = UITheme.styledButton("Add Supplier", UITheme.NAV_SUPPLIERS);
        JButton editBtn = UITheme.styledButton("Edit Selected", UITheme.NAV_MENU_MGMT);
        JButton deleteBtn = UITheme.styledButton("Delete Selected", UITheme.ACCENT_RED);
        JButton refreshBtn = UITheme.styledButton("Refresh", UITheme.FUNC_SUPPORT);

        addBtn.addActionListener(e -> openEditor(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) openEditor(loaded.get(row));
            else JOptionPane.showMessageDialog(this, "Select a supplier first.");
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a supplier first.");
                return;
            }
            Supplier s = loaded.get(row);
            int confirm = JOptionPane.showConfirmDialog(this, "Delete supplier \"" + s.getName() + "\"?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                supplierDAO.deleteSupplier(s.getId());
                loadTable();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        refreshBtn.addActionListener(e -> loadTable());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(deleteBtn);
        buttons.add(refreshBtn);
        add(buttons, BorderLayout.SOUTH);

        loadTable();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            loaded = supplierDAO.getAllSuppliers();
            for (Supplier s : loaded) {
                tableModel.addRow(new Object[]{
                    s.getId(), s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress()
                });
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load suppliers: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEditor(Supplier existing) {
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "");
        JTextField contactField = new JTextField(existing != null ? existing.getContactPerson() : "");
        JTextField phoneField = new JTextField(existing != null ? existing.getPhone() : "");
        JTextField emailField = new JTextField(existing != null ? existing.getEmail() : "");
        JTextField addressField = new JTextField(existing != null ? existing.getAddress() : "");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Supplier Name:")); form.add(nameField);
        form.add(new JLabel("Contact Person:")); form.add(contactField);
        form.add(new JLabel("Phone:")); form.add(phoneField);
        form.add(new JLabel("Email:")); form.add(emailField);
        form.add(new JLabel("Address:")); form.add(addressField);

        int result = JOptionPane.showConfirmDialog(this, form,
            existing == null ? "Add Supplier" : "Edit Supplier",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier name is required.", "Validation error", JOptionPane.WARNING_MESSAGE);
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

        Supplier s = existing != null ? existing : new Supplier();
        s.setName(nameField.getText().trim());
        s.setContactPerson(contactField.getText().trim());
        s.setPhone(phone);
        s.setEmail(email);
        s.setAddress(addressField.getText().trim());

        try {
            supplierDAO.saveSupplier(s);
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
