package com.possystem.gui;

import com.possystem.model.User;
import com.possystem.util.CoffeeShopScenePanel;
import com.possystem.util.CoffeeVibeHeaderPanel;
import com.possystem.util.UITheme;

import javax.swing.*;
import java.awt.*;

public class MainDashboard extends JFrame {

    private final User currentUser;
    private final JPanel contentArea = new JPanel(new BorderLayout());

    public MainDashboard(User currentUser) {
        this.currentUser = currentUser;
        setTitle("NY Coffee Co. - " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        add(buildSidebar(), BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);

        showPanel(new POSPanel(currentUser));
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(180, 0));
        sidebar.setBackground(new Color(35, 45, 60));

        sidebar.add(navButton("POS / Checkout", () -> showPanel(new POSPanel(currentUser))));
        sidebar.add(navButton("Menu Management", UITheme.NAV_MENU_MGMT, () -> showPanel("Menu Management", new MenuManagementPanel())));
        sidebar.add(navButton("Inventory", UITheme.NAV_INVENTORY, () -> showPanel("Inventory", new InventoryPanel(currentUser))));
        sidebar.add(navButton("Time Clock", UITheme.NAV_TIMECLOCK, () -> showPanel("Time Clock", new TimeClockPanel(currentUser))));
        sidebar.add(navButton("Delivery Queue", UITheme.NAV_DELIVERY, () -> showPanel("Delivery Queue", new DeliveryPanel())));
        sidebar.add(navButton("Recipes", UITheme.NAV_RECIPES, () -> showPanel("Recipes", new RecipePanel(currentUser))));

        if (currentUser.isManagerOrAbove()) {
            sidebar.add(navButton("Reports", UITheme.NAV_REPORTS, () -> showPanel("Reports", new ReportsPanel())));
            sidebar.add(navButton("Employees", UITheme.NAV_EMPLOYEES, () -> showPanel("Employees", new EmployeePanel())));
            sidebar.add(navButton("Payroll", UITheme.NAV_PAYROLL, () -> showPanel("Payroll", new PayrollPanel())));
        }
        if (currentUser.isAdmin()) {
            sidebar.add(navButton("Suppliers", UITheme.NAV_SUPPLIERS, () -> showPanel("Suppliers", new SupplierPanel())));
        }

        sidebar.add(navButton("About", UITheme.NAV_ABOUT, () -> showPanel("About", new AboutPanel())));

        sidebar.add(new CoffeeShopScenePanel());
        sidebar.add(navButton("Log Out", this::logout));
        return sidebar;
    }

    private JButton navButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(180, 44));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    /** Same as {@link #navButton(String, Runnable)} but painted with a distinct flat color
     *  (white bold text, subtle hover-darken) so each item in the sidebar reads at a glance -
     *  matching the colorful-per-section look used throughout the rest of the app. */
    private JButton navButton(String label, Color color, Runnable action) {
        JButton btn = UITheme.styledButton(label, color);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(180, 44));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void showPanel(JPanel panel) {
        contentArea.removeAll();
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    /** Same as {@link #showPanel(JPanel)}, but wraps the panel with a coffee-shop-vibe title
     *  banner (warm gradient, coffee beans, steaming cup) so every non-POS screen in the app
     *  feels branded and consistent, without touching that screen's own internal layout. */
    private void showPanel(String title, JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new CoffeeVibeHeaderPanel(title), BorderLayout.NORTH);
        wrapper.add(panel, BorderLayout.CENTER);
        showPanel(wrapper);
    }

    private void placeholder(String name) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("  " + name + " screen — build this next.", SwingConstants.CENTER), BorderLayout.CENTER);
        showPanel(name, p);
    }

    private void logout() {
        new LoginFrame().setVisible(true);
        dispose();
    }
}
