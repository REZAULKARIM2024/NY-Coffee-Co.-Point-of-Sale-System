package com.possystem.gui;

import com.possystem.dao.MenuItemDAO;
import com.possystem.dao.RecipeDAO;
import com.possystem.model.MenuItem;
import com.possystem.model.RecipeStep;
import com.possystem.model.User;
import com.possystem.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Prep-instruction viewer/editor: pick a menu item + language, see the numbered steps.
 * Anyone can read steps (handy at the station); only managers/admins can add, edit, or
 * delete them.
 */
public class RecipePanel extends JPanel {

    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final User currentUser;

    private final JComboBox<MenuItem> itemBox = new JComboBox<>();
    private final JComboBox<String> languageBox = new JComboBox<>(new String[]{"en", "es", "fr", "zh"});
    private final DefaultListModel<RecipeStep> listModel = new DefaultListModel<>();
    private final JList<RecipeStep> stepList = new JList<>(listModel);

    public RecipePanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        add(buildSelectorRow(), BorderLayout.NORTH);

        Color stripe = UITheme.tint(UITheme.NAV_RECIPES, 0.88);
        stepList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel("<html><body style='width:400px'><b><font color='#" +
                String.format("%06X", UITheme.NAV_RECIPES.darker().getRGB() & 0xFFFFFF) + "'>Step " +
                value.getStepNumber() + ":</font></b> " + escape(value.getInstruction()) + "</body></html>");
            label.setOpaque(true);
            label.setBackground(isSelected ? UITheme.NAV_RECIPES : (index % 2 == 0 ? Color.WHITE : stripe));
            if (isSelected) label.setForeground(Color.WHITE);
            label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            return label;
        });
        add(new JScrollPane(stepList), BorderLayout.CENTER);

        if (currentUser.isManagerOrAbove()) {
            add(buildEditButtons(), BorderLayout.SOUTH);
        }

        try {
            List<MenuItem> items = menuItemDAO.getAllMenuItems();
            for (MenuItem m : items) itemBox.addItem(m);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load menu items: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        itemBox.addActionListener(e -> loadSteps());
        languageBox.addActionListener(e -> loadSteps());
        loadSteps();
    }

    private JComponent buildSelectorRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.add(new JLabel("Menu Item:"));
        itemBox.setPreferredSize(new Dimension(220, 28));
        row.add(itemBox);
        row.add(new JLabel("Language:"));
        languageBox.setEditable(true);
        languageBox.setPreferredSize(new Dimension(80, 28));
        row.add(languageBox);
        return row;
    }

    private JComponent buildEditButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        JButton addBtn = UITheme.styledButton("Add Step", UITheme.NAV_RECIPES);
        JButton editBtn = UITheme.styledButton("Edit Selected", UITheme.NAV_EMPLOYEES);
        JButton deleteBtn = UITheme.styledButton("Delete Selected", UITheme.ACCENT_RED);

        addBtn.addActionListener(e -> addStep());
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());

        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(deleteBtn);
        return buttons;
    }

    private MenuItem selectedItem() {
        return (MenuItem) itemBox.getSelectedItem();
    }

    private String selectedLanguage() {
        Object sel = languageBox.getSelectedItem();
        String lang = sel == null ? "" : sel.toString().trim().toLowerCase();
        return lang.isEmpty() ? "en" : lang;
    }

    private void loadSteps() {
        listModel.clear();
        MenuItem item = selectedItem();
        if (item == null) return;
        try {
            List<RecipeStep> steps = recipeDAO.getSteps(item.getId(), selectedLanguage());
            for (RecipeStep s : steps) listModel.addElement(s);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load recipe steps: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addStep() {
        MenuItem item = selectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Select a menu item first.");
            return;
        }
        String text = promptForText("New step instruction:", "");
        if (text == null) return;
        try {
            recipeDAO.addStep(item.getId(), selectedLanguage(), text);
            loadSteps();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to add step: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelected() {
        RecipeStep selected = stepList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a step first.");
            return;
        }
        String text = promptForText("Edit step " + selected.getStepNumber() + ":", selected.getInstruction());
        if (text == null) return;
        try {
            recipeDAO.updateStepText(selected.getId(), text);
            loadSteps();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to update step: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        RecipeStep selected = stepList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a step first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete step " + selected.getStepNumber() + "?",
            "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            recipeDAO.deleteStep(selected.getId());
            loadSteps();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete step: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String promptForText(String title, String initial) {
        JTextArea area = new JTextArea(initial, 5, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(area), title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        String text = area.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Instruction text is required.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return text;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
