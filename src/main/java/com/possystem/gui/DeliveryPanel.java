package com.possystem.gui;

import com.possystem.dao.DeliveryDAO;
import com.possystem.model.Delivery;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Delivery queue screen: UNASSIGNED -> ASSIGNED -> PICKED_UP -> DELIVERED.
 * Orders land here automatically (as UNASSIGNED) when checked out with order type DELIVERY.
 */
public class DeliveryPanel extends JPanel {

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy h:mm a");
    private final JCheckBox showCompletedBox = new JCheckBox("Show completed deliveries");

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Order #", "Customer", "Phone", "Address", "Total", "Status", "Assigned To", "Placed"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Delivery> loaded;

    public DeliveryPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        JTable table = new JTable(tableModel);
        UITheme.styleTable(table, UITheme.NAV_DELIVERY);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton assignBtn = UITheme.styledButton("Assign to Driver...", UITheme.NAV_DELIVERY);
        JButton pickedUpBtn = UITheme.styledButton("Mark Picked Up", UITheme.FEATURED_LEMONADE);
        JButton deliveredBtn = UITheme.styledButton("Mark Delivered", UITheme.NAV_RECIPES);
        JButton refreshBtn = UITheme.styledButton("Refresh", UITheme.FUNC_SUPPORT);

        assignBtn.addActionListener(e -> withSelected(table, this::assignDialog));
        pickedUpBtn.addActionListener(e -> withSelected(table, d -> updateStatus(d, "PICKED_UP")));
        deliveredBtn.addActionListener(e -> withSelected(table, d -> updateStatus(d, "DELIVERED")));
        refreshBtn.addActionListener(e -> loadTable());
        showCompletedBox.addActionListener(e -> loadTable());
        showCompletedBox.setOpaque(false);
        showCompletedBox.setFont(showCompletedBox.getFont().deriveFont(Font.BOLD));
        showCompletedBox.setForeground(UITheme.NAV_DELIVERY.darker());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(assignBtn);
        buttons.add(pickedUpBtn);
        buttons.add(deliveredBtn);
        buttons.add(refreshBtn);
        buttons.add(showCompletedBox);
        add(buttons, BorderLayout.SOUTH);

        loadTable();
    }

    private interface DeliveryAction { void run(Delivery d); }

    private void withSelected(JTable table, DeliveryAction action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a delivery first.");
            return;
        }
        action.run(loaded.get(row));
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            loaded = showCompletedBox.isSelected() ? deliveryDAO.getAllDeliveries() : deliveryDAO.getOpenQueue();
            for (Delivery d : loaded) {
                tableModel.addRow(new Object[]{
                    d.getOrderId(),
                    d.getCustomerName() != null ? d.getCustomerName() : "(walk-up/phone)",
                    d.getCustomerPhone() != null ? d.getCustomerPhone() : "",
                    d.getCustomerAddress(),
                    "$" + d.getOrderTotal(),
                    d.getStatus(),
                    d.getAssignedTo() != null ? d.getAssignedTo() : "",
                    fmt.format(d.getCreatedAt())
                });
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load delivery queue: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void assignDialog(Delivery delivery) {
        String driver = JOptionPane.showInputDialog(this, "Driver / staff name:", delivery.getAssignedTo());
        if (driver == null) return;
        driver = driver.trim();
        if (driver.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Driver name is required.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            deliveryDAO.assign(delivery.getId(), driver);
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to assign delivery: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus(Delivery delivery, String status) {
        if (status.equals("PICKED_UP") && delivery.getAssignedTo() == null) {
            JOptionPane.showMessageDialog(this, "Assign a driver before marking this picked up.",
                "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            deliveryDAO.updateStatus(delivery.getId(), status);
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to update delivery: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
