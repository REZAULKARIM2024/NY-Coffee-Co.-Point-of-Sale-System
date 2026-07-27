package com.possystem.gui;

import com.possystem.dao.IngredientDAO;
import com.possystem.model.Ingredient;
import com.possystem.model.User;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class InventoryPanel extends JPanel {

    private final IngredientDAO ingredientDAO = new IngredientDAO();
    private final User currentUser;
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"ID", "Ingredient", "Unit", "Stock", "Low Threshold", "Low Stock?"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Ingredient> loaded;

    public InventoryPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        JTable table = new JTable(tableModel);
        UITheme.styleTable(table, UITheme.NAV_INVENTORY);
        // Low-stock rows get a warm red tint (in the last column) regardless of stripe, so
        // they still stand out against the normal accent-tinted alternating rows.
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                boolean low = "YES".equals(value);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(low ? new Color(255, 205, 205) : (row % 2 == 0 ? Color.WHITE : UITheme.tint(UITheme.NAV_INVENTORY, 0.88)));
                    c.setForeground(low ? new Color(150, 20, 20) : Color.BLACK);
                    setFont(getFont().deriveFont(low ? Font.BOLD : Font.PLAIN));
                }
                return c;
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton adjustBtn = UITheme.styledButton("Adjust Stock", UITheme.NAV_INVENTORY);
        adjustBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) openAdjustDialog(loaded.get(row));
            else JOptionPane.showMessageDialog(this, "Select an ingredient first.");
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(adjustBtn);
        add(buttons, BorderLayout.SOUTH);

        loadTable();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            loaded = ingredientDAO.getAllIngredients();
            for (Ingredient i : loaded) {
                tableModel.addRow(new Object[]{
                    i.getId(), i.getName(), i.getUnit(), i.getStockQuantity(),
                    i.getLowStockThreshold(), i.isLowStock() ? "YES" : ""
                });
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load inventory: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAdjustDialog(Ingredient ingredient) {
        String[] reasons = {"PURCHASE_IN", "WASTE", "CORRECTION"};
        JComboBox<String> reasonBox = new JComboBox<>(reasons);
        JTextField amountField = new JTextField("0");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Ingredient:")); form.add(new JLabel(ingredient.getName()));
        form.add(new JLabel("Reason:")); form.add(reasonBox);
        form.add(new JLabel("Amount (+/-):")); form.add(amountField);

        int result = JOptionPane.showConfirmDialog(this, form, "Adjust Stock",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = (String) reasonBox.getSelectedItem();
        // WASTE and CORRECTION-down conventionally reduce stock; let the sign the user enters drive it directly.
        try {
            ingredientDAO.adjustStock(ingredient.getId(), amount, reason, currentUser.getId());
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Adjustment failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
