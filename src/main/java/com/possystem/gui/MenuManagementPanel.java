package com.possystem.gui;

import com.possystem.dao.MenuItemDAO;
import com.possystem.model.Category;
import com.possystem.model.MenuItem;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class MenuManagementPanel extends JPanel {

    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"ID", "Category", "Name", "Price", "Cost", "Active"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<MenuItem> loadedItems;

    public MenuManagementPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        JTable table = new JTable(tableModel);
        UITheme.styleTable(table, UITheme.NAV_MENU_MGMT);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = UITheme.styledButton("Add Item", UITheme.NAV_MENU_MGMT);
        JButton editBtn = UITheme.styledButton("Edit Selected", UITheme.DEPT_TAB_ACTIVE);
        addBtn.addActionListener(e -> openEditor(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) openEditor(loadedItems.get(row));
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(addBtn);
        buttons.add(editBtn);
        add(buttons, BorderLayout.SOUTH);

        loadTable();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            loadedItems = menuItemDAO.getAllMenuItems();
            for (MenuItem m : loadedItems) {
                tableModel.addRow(new Object[]{
                    m.getId(), m.getCategoryName(), m.getName(), m.getPrice(), m.getCost(), m.isActive()
                });
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load menu items: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEditor(MenuItem existing) {
        List<Category> categories = menuItemDAO.getAllCategories();
        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No categories exist yet. Add one to the categories table first.");
            return;
        }

        JComboBox<Category> categoryBox = new JComboBox<>(categories.toArray(new Category[0]));
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "");
        JTextField priceField = new JTextField(existing != null ? existing.getPrice().toString() : "0.00");
        JTextField costField = new JTextField(existing != null ? existing.getCost().toString() : "0.00");
        JCheckBox activeBox = new JCheckBox("Active", existing == null || existing.isActive());

        if (existing != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == existing.getCategoryId()) categoryBox.setSelectedIndex(i);
            }
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Category:")); form.add(categoryBox);
        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Price:")); form.add(priceField);
        form.add(new JLabel("Cost:")); form.add(costField);
        form.add(new JLabel("")); form.add(activeBox);

        int result = JOptionPane.showConfirmDialog(this, form,
            existing == null ? "Add Menu Item" : "Edit Menu Item",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal price, cost;
        try {
            price = new BigDecimal(priceField.getText().trim());
            cost = new BigDecimal(costField.getText().trim());
            if (price.compareTo(BigDecimal.ZERO) < 0 || cost.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException("negative value");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price and cost must be non-negative numbers.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MenuItem item = existing != null ? existing : new MenuItem();
        item.setName(nameField.getText().trim());
        item.setPrice(price);
        item.setCost(cost);
        item.setActive(activeBox.isSelected());
        item.setCategoryId(((Category) categoryBox.getSelectedItem()).getId());

        try {
            menuItemDAO.saveMenuItem(item);
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
