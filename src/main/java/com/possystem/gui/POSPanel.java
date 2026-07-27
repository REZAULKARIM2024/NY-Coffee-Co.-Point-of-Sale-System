package com.possystem.gui;

import com.possystem.dao.CustomerDAO;
import com.possystem.dao.DeliveryDAO;
import com.possystem.dao.EmployeeDAO;
import com.possystem.dao.MenuItemDAO;
import com.possystem.dao.OperationsDAO;
import com.possystem.dao.OrderDAO;
import com.possystem.dao.PayrollDAO;
import com.possystem.dao.PricingOptionsDAO;
import com.possystem.model.CartItem;
import com.possystem.model.Category;
import com.possystem.model.Customer;
import com.possystem.model.Employee;
import com.possystem.model.MenuItem;
import com.possystem.model.Modifier;
import com.possystem.model.Size;
import com.possystem.model.User;
import com.possystem.service.POSService;
import com.possystem.util.I18n;
import com.possystem.util.NYCSkylinePanel;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * POS screen laid out like a typical counter-service terminal:
 *   NORTH  = staff/time header + a row of department tabs (Beverages, Bakery, Sandwiches, ...)
 *   WEST   = receipt-style cart + totals + payment actions
 *   CENTER = item grid (grouped by section headers), or a size/temp/modifier "customize" screen
 *   EAST   = subcategory sidebar for whichever department is selected (e.g. Beverages ->
 *            Coffee/Espresso/Teas/Refreshers/Frozen/Other Beverages)
 */
public class POSPanel extends JPanel {

    private final User currentUser;
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final PricingOptionsDAO pricingOptionsDAO = new PricingOptionsDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final OperationsDAO operationsDAO = new OperationsDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final PayrollDAO payrollDAO = new PayrollDAO();
    private final POSService posService = new POSService();
    private OrderConfirmationBoardFrame displayA;
    private OrderConfirmationBoardFrame displayB;
    private Employee actingEmployee; // set by "Assign Employee Id" — session-scoped, used as a sensible default for drawer/till actions
    private final JComboBox<String> orderTypeBox = new JComboBox<>(new String[]{"DINE_IN", "PICKUP", "DELIVERY"});
    private final JComboBox<String> orderSourceBox = new JComboBox<>(new String[]{"IN_STORE", "PHONE", "ONLINE"});
    private String deliveryAddress;
    private final java.util.List<CartItem> cart = new java.util.ArrayList<>();
    private List<Size> allSizes = new ArrayList<>();
    private List<Modifier> allModifiers = new ArrayList<>();
    private Customer pendingCustomer; // set when staff registers/looks up a loyalty customer for this order
    private final JButton loyaltyBtn = UITheme.styledButton("+ Loyalty Customer", UITheme.KEYPAD_BG);

    private final CardLayout centerCards = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerCards);
    private final JPanel cartListPanel = new JPanel();
    private final JLabel subtotalLabel = new JLabel("Subtotal   $0.00");
    private final JLabel taxLabel = new JLabel("Tax            $0.00");
    private final JLabel totalLabel = new JLabel("Total         $0.00");
    private final JTextField discountField = new JTextField("0.00", 5);

    private MenuItem pendingItem;      // item currently being customized
    private Size pendingSize;
    private String pendingTemp = "Hot";
    private final List<Modifier> pendingModifiers = new ArrayList<>();

    // ---- Department tabs (NORTH) + subcategory sidebar (EAST) ----
    private final JPanel deptTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final java.util.Map<String, JButton> deptTabButtons = new java.util.LinkedHashMap<>();
    private final JPanel sidebarContainer = new JPanel(new BorderLayout());
    private final java.util.Map<String, JButton> sidebarButtons = new java.util.LinkedHashMap<>();
    private Category currentDepartment;
    private Category currentSubcategory;
    private static final String[] DEPARTMENT_TAB_ORDER =
        {"Beverages", "Featured", "Bakery", "Sandwiches", "Retail", "Local", "Functions"};

    // ---- Payments screen (opened from the PAY button) ----
    private final java.util.Map<String, JButton> paymentsSidebarButtons = new java.util.LinkedHashMap<>();
    private static final String[] PAYMENTS_SIDEBAR_ORDER =
        {"Payments", "Other Payments", "Coupons/Discounts", "Gift Card Functions", "Service Charges", "Delivery Payments"};
    private final JTextField paymentAmountField = new JTextField();
    private boolean taxExempt = false;

    // ---- Language + Help (5 languages: English/Bangla/Hindi/Spanish/French) ----
    private final JLabel headerLeftLabel = new JLabel();
    private final int chkNumber = 1000 + (int) (Math.random() * 9000);
    private final JButton langBtn = new JButton();
    private final JButton helpBtn = UITheme.styledButton("Help", UITheme.KEYPAD_BG);
    private String activeFunctionsGroup;   // non-null while the Functions tab is showing (last group clicked)
    private String activePaymentsGroup;    // non-null while the Payments screen is showing (last group clicked)

    // ---- Cart column fields (need direct refs so language switches can re-label them) ----
    private final JLabel orderTypeLabel = new JLabel();
    private final JLabel sourceLabel = new JLabel();
    private final JLabel discountLabel = new JLabel();
    private final JButton removeBtn = UITheme.styledButton("Remove Last", UITheme.ACCENT_RED);
    private final JButton cancelBtn = UITheme.styledButton("Cancel Order", UITheme.ACCENT_RED);
    private final JButton payBtn = UITheme.styledButton("PAY", UITheme.PAY_GREEN);

    public POSPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());

        try {
            allSizes = pricingOptionsDAO.getAllSizes();
            allModifiers = pricingOptionsDAO.getAllModifiers();
        } catch (RuntimeException ignored) { /* customize screen will just show no options */ }

        add(buildHeaderArea(), BorderLayout.NORTH);
        add(buildCartColumn(), BorderLayout.WEST);
        sidebarContainer.setPreferredSize(new Dimension(160, 0));
        add(sidebarContainer, BorderLayout.EAST);

        centerPanel.add(gridContainer, "GRID");
        centerPanel.add(buildCustomizePanel(), "CUSTOMIZE");
        add(centerPanel, BorderLayout.CENTER);

        initDepartmentTabs();
        refreshCartList();
        I18n.addListener(this::refreshLanguage);
    }

    // ---------- HEADER + DEPARTMENT TAB BAR (NORTH) ----------
    private JComponent buildHeaderArea() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(buildHeader(), BorderLayout.NORTH);
        wrap.add(buildDeptTabBar(), BorderLayout.SOUTH);
        return wrap;
    }

    private JComponent buildHeader() {
        // Subtle NYC skyline strip painted behind the header bar — same dusk-navy palette as
        // HEADER_BG so it reads as texture, not a competing background.
        JComponent header = new NYCSkylinePanel(true);
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel leftSide = new JPanel();
        leftSide.setOpaque(false);
        leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));

        JLabel brandLabel = new JLabel("NY Coffee Co.");
        brandLabel.setForeground(new Color(230, 180, 70));
        brandLabel.setFont(brandLabel.getFont().deriveFont(Font.BOLD, 15f));
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftSide.add(brandLabel);

        updateHeaderLeftText();
        headerLeftLabel.setForeground(Color.WHITE);
        headerLeftLabel.setFont(headerLeftLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLeftLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftSide.add(headerLeftLabel);

        header.add(leftSide, BorderLayout.WEST);

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightSide.setOpaque(false);

        langBtn.setText(I18n.current().nativeName + " v");
        langBtn.setFocusPainted(false);
        langBtn.addActionListener(e -> showLanguagePicker());
        rightSide.add(langBtn);

        helpBtn.setText(I18n.t("Help"));
        helpBtn.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            new HelpDialog(w instanceof Frame ? (Frame) w : null).setVisible(true);
        });
        rightSide.add(helpBtn);

        JLabel right = new JLabel(new SimpleDateFormat("MM/dd/yyyy h:mm a").format(new Date()));
        right.setForeground(Color.LIGHT_GRAY);
        rightSide.add(right);

        header.add(rightSide, BorderLayout.EAST);
        return header;
    }

    private void updateHeaderLeftText() {
        headerLeftLabel.setText(currentUser.getFullName() + "   |   " + I18n.t("Eat In") + "   |   CHK " + chkNumber);
    }

    /** Small popup letting staff pick one of the 5 supported languages by native name. */
    private void showLanguagePicker() {
        I18n.Lang[] options = I18n.Lang.values();
        String[] names = new String[options.length];
        for (int i = 0; i < options.length; i++) names[i] = options[i].nativeName;
        String choice = (String) JOptionPane.showInputDialog(this, I18n.t("Select language:"),
                I18n.t("Language"), JOptionPane.PLAIN_MESSAGE, null, names, I18n.current().nativeName);
        if (choice == null) return;
        for (I18n.Lang lang : options) {
            if (lang.nativeName.equals(choice)) { I18n.setLanguage(lang); break; }
        }
    }

    /** Redraws every piece of on-screen text that was built with an I18n key, after a
     *  language switch — dept tabs, cart column chrome, and whatever screen is currently
     *  active (item grid, Functions group, or Payments group). Product/category names loaded
     *  from the database are intentionally left as-is. */
    private void refreshLanguage() {
        langBtn.setText(I18n.current().nativeName + " v");
        helpBtn.setText(I18n.t("Help"));
        updateHeaderLeftText();

        for (var entry : deptTabButtons.entrySet()) {
            entry.getValue().setText(I18n.t(entry.getKey()));
        }

        orderTypeLabel.setText(I18n.t("Order Type:"));
        sourceLabel.setText(I18n.t("Source:"));
        discountLabel.setText(I18n.t("Discount $:"));
        if (pendingCustomer == null) loyaltyBtn.setText(I18n.t("+ Loyalty Customer"));
        removeBtn.setText(I18n.t("Remove Last"));
        cancelBtn.setText(I18n.t("Cancel Order"));
        payBtn.setText(I18n.t("PAY"));
        recalcTotals();

        if (activePaymentsGroup != null) {
            rebuildPaymentsSidebar(activePaymentsGroup);
            showPaymentsGroup(activePaymentsGroup);
        } else if (activeFunctionsGroup != null) {
            rebuildFunctionsSidebar(activeFunctionsGroup);
            showFunctionsGroup(activeFunctionsGroup);
        } else if (currentDepartment != null) {
            List<Category> children = menuItemDAO.getChildCategories(currentDepartment.getId());
            rebuildSidebar(children.isEmpty() ? null : children);
            if (currentSubcategory != null) {
                highlightSidebarItem(currentSubcategory.getName());
                showItemsForCategory(currentSubcategory);
            } else {
                showItemsForCategory(currentDepartment);
            }
        }
    }

    private JComponent buildDeptTabBar() {
        deptTabBar.setBackground(UITheme.DEPT_TAB_BAR_BG);
        deptTabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UITheme.DEPT_TAB_ACTIVE));
        return deptTabBar;
    }

    /** Loads the real (DB) departments and interleaves the two non-DB placeholder tabs
     *  (Featured, Functions) at their positions in the reference layout. */
    private void initDepartmentTabs() {
        try {
            List<Category> topLevel = menuItemDAO.getTopLevelCategories();
            java.util.Map<String, Category> byName = new java.util.HashMap<>();
            for (Category c : topLevel) byName.put(c.getName(), c);

            for (String name : DEPARTMENT_TAB_ORDER) {
                Category dept = byName.get(name);
                JButton b = UITheme.styledButton(I18n.t(name), UITheme.DEPT_TAB_BG);
                b.setForeground(UITheme.DEPT_TAB_TEXT);
                b.setPreferredSize(new Dimension(120, 38));
                b.addActionListener(e -> {
                    if (dept == null) selectPlaceholderTab(name);
                    else selectDepartment(dept);
                });
                deptTabButtons.put(name, b);
                deptTabBar.add(b);
            }

            Category initial = byName.containsKey("Beverages") ? byName.get("Beverages")
                : (topLevel.isEmpty() ? null : topLevel.get(0));
            if (initial != null) selectDepartment(initial);
        } catch (RuntimeException e) {
            gridContainer.removeAll();
            gridContainer.add(new JLabel("Menu load failed: " + e.getMessage()), BorderLayout.CENTER);
            gridContainer.revalidate();
        }
    }

    private void highlightDeptTab(String activeName) {
        for (var entry : deptTabButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeName);
            entry.getValue().setBackground(active ? UITheme.DEPT_TAB_ACTIVE : UITheme.DEPT_TAB_BG);
            entry.getValue().setForeground(active ? Color.WHITE : UITheme.DEPT_TAB_TEXT);
        }
    }

    private void selectDepartment(Category dept) {
        currentDepartment = dept;
        currentSubcategory = null;
        activeFunctionsGroup = null;
        activePaymentsGroup = null;
        highlightDeptTab(dept.getName());
        List<Category> children = menuItemDAO.getChildCategories(dept.getId());
        if (children.isEmpty()) {
            rebuildSidebar(null);
            showItemsForCategory(dept);
        } else {
            rebuildSidebar(children);
            selectSubcategory(children.get(0));
        }
        centerCards.show(centerPanel, "GRID");
    }

    private void selectSubcategory(Category subcat) {
        currentSubcategory = subcat;
        highlightSidebarItem(subcat.getName());
        showItemsForCategory(subcat);
    }

    private void selectPlaceholderTab(String name) {
        if ("Functions".equals(name)) {
            selectFunctionsTab();
            return;
        }
        currentDepartment = null;
        currentSubcategory = null;
        highlightDeptTab(name);
        rebuildSidebar(null);

        JLabel msg = new JLabel(name + " — coming soon", SwingConstants.CENTER);
        msg.setFont(msg.getFont().deriveFont(Font.BOLD, 22f));
        msg.setForeground(UITheme.SECTION_HEADER_COLOR);

        gridContainer.removeAll();
        gridContainer.add(msg, BorderLayout.CENTER);
        gridContainer.revalidate();
        gridContainer.repaint();
        centerCards.show(centerPanel, "GRID");
    }

    // ---------- FUNCTIONS TAB (manager function board — client-side, not purchasable) ----------
    private final java.util.Map<String, JButton> functionsSidebarButtons = new java.util.LinkedHashMap<>();
    private static final String[] FUNCTIONS_SIDEBAR_ORDER =
        {"Manager", "Daily Shift Functions", "Reports", "All Open Checks", "Phone Orders",
         "DT Orders", "OTG Orders", "Order Confirm Board", "SUPPORT"};

    private void selectFunctionsTab() {
        currentDepartment = null;
        currentSubcategory = null;
        activeFunctionsGroup = "Manager";
        activePaymentsGroup = null;
        highlightDeptTab("Functions");
        rebuildFunctionsSidebar("Manager");
        showFunctionsGroup("Manager");
        centerCards.show(centerPanel, "GRID");
    }

    private void rebuildFunctionsSidebar(String activeName) {
        functionsSidebarButtons.clear();
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        for (String name : FUNCTIONS_SIDEBAR_ORDER) {
            JButton b = UITheme.styledButton(I18n.t(name), UITheme.SIDEBAR_ITEM_BG);
            b.setForeground(UITheme.DEPT_TAB_TEXT);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(150, 56));
            b.addActionListener(e -> {
                activeFunctionsGroup = name;
                rebuildFunctionsSidebar(name);
                showFunctionsGroup(name);
            });
            functionsSidebarButtons.put(name, b);
            inner.add(b);
            inner.add(Box.createVerticalStrut(6));
        }
        inner.add(Box.createVerticalGlue());

        JPanel bg = UITheme.gradientPanel(UITheme.SIDEBAR_BG, new Color(250, 226, 196));
        bg.add(inner, BorderLayout.CENTER);

        sidebarContainer.removeAll();
        sidebarContainer.add(bg, BorderLayout.CENTER);
        sidebarContainer.revalidate();
        sidebarContainer.repaint();

        for (var entry : functionsSidebarButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeName);
            entry.getValue().setBackground(active ? UITheme.SIDEBAR_ACTIVE : UITheme.SIDEBAR_ITEM_BG);
            entry.getValue().setForeground(active ? Color.WHITE : UITheme.DEPT_TAB_TEXT);
        }
    }

    /** One function-board "row": a section header followed by a row of colorful buttons, each
     *  wired to real, working logic — DB-backed actions, live reports, or genuine app behavior.
     *  No more canned "X completed" popups. */
    private void addFunctionRow(JPanel content, String header, Color color, LinkedHashMap<String, Runnable> actions) {
        if (header != null) {
            JLabel h = new JLabel(I18n.t(header));
            h.setForeground(UITheme.SECTION_HEADER_COLOR);
            h.setFont(h.getFont().deriveFont(Font.BOLD, 16f));
            h.setAlignmentX(Component.LEFT_ALIGNMENT);
            h.setBorder(BorderFactory.createEmptyBorder(10, 2, 6, 0));
            content.add(h);
        }

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (var entry : actions.entrySet()) {
            String label = I18n.t(entry.getKey());
            JButton btn = UITheme.styledButton("<html><center>" + label + "</center></html>", color);
            btn.setPreferredSize(new Dimension(150, 70));
            Runnable action = entry.getValue();
            btn.addActionListener(e -> runSafely(action));
            row.add(btn);
        }
        content.add(row);
    }

    /** Like addFunctionRow, but each button gets its own color (e.g. a row mixing green
     *  "activate" buttons with a red "deactivate" button). */
    private void addFunctionRowMixed(JPanel content, String header, LinkedHashMap<String, Runnable> actions, Color[] colors) {
        if (header != null) {
            JLabel h = new JLabel(I18n.t(header));
            h.setForeground(UITheme.SECTION_HEADER_COLOR);
            h.setFont(h.getFont().deriveFont(Font.BOLD, 16f));
            h.setAlignmentX(Component.LEFT_ALIGNMENT);
            h.setBorder(BorderFactory.createEmptyBorder(10, 2, 6, 0));
            content.add(h);
        }
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        int i = 0;
        for (var entry : actions.entrySet()) {
            String label = I18n.t(entry.getKey());
            JButton btn = UITheme.styledButton("<html><center>" + label + "</center></html>", colors[i++]);
            btn.setPreferredSize(new Dimension(150, 70));
            Runnable action = entry.getValue();
            btn.addActionListener(e -> runSafely(action));
            row.add(btn);
        }
        content.add(row);
    }

    /** A full-width banner button (e.g. "View Status", "Cash Management Dashboard") that spans
     *  the width of the function grid, wired to a real action. */
    private void addFunctionBanner(JPanel content, String rawLabel, Color color, Runnable action) {
        String label = I18n.t(rawLabel);
        JButton btn = UITheme.styledButton(label, color);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        btn.setPreferredSize(new Dimension(600, 54));
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 15f));
        btn.addActionListener(e -> runSafely(action));
        content.add(Box.createVerticalStrut(8));
        content.add(btn);
        content.add(Box.createVerticalStrut(4));
    }

    /** Builds a label->action map concisely: actions("Label1", r1, "Label2", r2, ...). */
    private static LinkedHashMap<String, Runnable> actions(Object... pairs) {
        LinkedHashMap<String, Runnable> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], (Runnable) pairs[i + 1]);
        return map;
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- Functions tab: shared dialog / DB helpers ----------

    private Integer currentEmployeeId() {
        if (actingEmployee != null) return actingEmployee.getId();
        return currentUser.getEmployeeId();
    }

    private void info(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private String promptText(String title, String initial) {
        Object s = JOptionPane.showInputDialog(this, title, "Input", JOptionPane.PLAIN_MESSAGE, null, null, initial);
        return s == null ? null : s.toString().trim();
    }

    private BigDecimal promptAmount(String title) {
        String s = promptText(title, "0.00");
        if (s == null) return null;
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.signum() < 0) throw new NumberFormatException();
            return v.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid non-negative amount.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private Employee pickEmployee(String title) {
        List<Employee> all = employeeDAO.getAllEmployees();
        if (all.isEmpty()) { info(title, "No employees found. Add one under Employees first."); return null; }
        return (Employee) JOptionPane.showInputDialog(this, title, title,
                JOptionPane.PLAIN_MESSAGE, null, all.toArray(), all.get(0));
    }

    private MenuItem pickMenuItemBySearch(String title) {
        String query = promptText(title + " — type part of the item name:", "");
        if (query == null || query.isEmpty()) return null;
        List<MenuItem> matches = new ArrayList<>();
        String q = query.toLowerCase();
        for (MenuItem m : menuItemDAO.getAllMenuItems()) {
            if (m.getName().toLowerCase().contains(q)) {
                matches.add(m);
                if (matches.size() >= 25) break;
            }
        }
        if (matches.isEmpty()) { info(title, "No items matched \"" + query + "\"."); return null; }
        return (MenuItem) JOptionPane.showInputDialog(this, "Select item:", title,
                JOptionPane.PLAIN_MESSAGE, null, matches.toArray(), matches.get(0));
    }

    /** Shows a scrollable table of rows in a dialog — the generic "report viewer" reused by
     *  every Reports button, so each one shows real data pulled straight from the database. */
    private void showReportDialog(String title, String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] row : rows) model.addRow(row);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, Math.min(420, 70 + rows.size() * 22)));
        JOptionPane.showMessageDialog(this, rows.isEmpty() ? new JLabel("  No data yet.  ") : scroll,
                title, JOptionPane.PLAIN_MESSAGE);
    }

    /** Lets staff pick one order from a list of [id, source, type, customer, cashier, total, placed] rows. */
    private Object[] pickOrderFromList(String title, List<Object[]> orders) {
        if (orders.isEmpty()) { info(title, "No matching orders found."); return null; }
        String[] labels = new String[orders.size()];
        for (int i = 0; i < orders.size(); i++) {
            Object[] o = orders.get(i);
            labels[i] = "#" + o[0] + "   " + o[3] + "   $" + o[5] + "   " + o[6];
        }
        String choice = (String) JOptionPane.showInputDialog(this, title, title,
                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
        if (choice == null) return null;
        return orders.get(Arrays.asList(labels).indexOf(choice));
    }

    private void doDrawerTxn(String type, String label) {
        BigDecimal amount = "NO_SALE".equals(type) ? BigDecimal.ZERO : promptAmount(label + " — amount:");
        if (amount == null) return;
        String reason = "NO_SALE".equals(type) ? "Drawer opened, no sale" : promptText(label + " — reason:", "");
        operationsDAO.recordDrawerTransaction(type, amount, reason, currentEmployeeId());
        info(label, label + " recorded.\nToday's " + label + " total: $" + operationsDAO.sumTodayDrawerTransactions(type));
    }

    private void doSafeTxn(String type, String label) {
        BigDecimal amount = promptAmount(label + " — amount:");
        if (amount == null) return;
        String notes = promptText(label + " — notes:", "");
        operationsDAO.recordSafeTransaction(type, amount, currentEmployeeId(), notes);
        info(label, label + " recorded.\nCurrent safe balance: $" + operationsDAO.currentSafeBalance());
    }

    private void doToggleSetting(String key, String label, String onValue, String offValue) {
        String current = operationsDAO.getSetting(key, offValue);
        String next = current.equals(onValue) ? offValue : onValue;
        operationsDAO.setSetting(key, next);
        info(label, label + " is now: " + next);
    }

    private void adjustClosedCheck() {
        Object[] order = pickOrderFromList("Adjust Closed Check — pick completed order", orderDAO.getOrdersByStatus("COMPLETED"));
        if (order == null) return;
        int orderId = (Integer) order[0];
        BigDecimal oldTotal = (BigDecimal) order[5];
        BigDecimal newTotal = promptAmount("New total for order #" + orderId + " (was $" + oldTotal + "):");
        if (newTotal == null) return;
        BigDecimal deltaDiscount = oldTotal.subtract(newTotal);
        orderDAO.adjustOrderTotals(orderId, deltaDiscount, newTotal);
        info("Adjust Closed Check", "Order #" + orderId + " total adjusted to $" + newTotal + ".");
    }

    private void beginPhoneOrder() {
        orderSourceBox.setSelectedItem("PHONE");
        orderTypeBox.setSelectedItem("PICKUP");
        JButton beverages = deptTabButtons.get("Beverages");
        if (beverages != null) beverages.doClick();
        info("Begin Phone Order", "New phone order started — source set to PHONE, type set to PICKUP.");
    }

    private void showDisplayStatus() {
        String statusA = displayA != null
                ? "ACTIVE since " + displayA.getActivatedAt().format(DateTimeFormatter.ofPattern("h:mm:ss a")) : "INACTIVE";
        String statusB = displayB != null
                ? "ACTIVE since " + displayB.getActivatedAt().format(DateTimeFormatter.ofPattern("h:mm:ss a")) : "INACTIVE";
        info("Order Confirmation Board Status", "Display A: " + statusA + "\nDisplay B: " + statusB);
    }

    private void showFunctionsGroup(String groupName) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        switch (groupName) {
            case "Manager":
                addFunctionRow(content, "Application Functions", UITheme.FUNC_APP, actions(
                    "Launch PMC", (Runnable) () -> info("Launch PMC",
                            "Property Management Console\n\nRegister: CHK " + chkNumber +
                            "\nCashier: " + currentUser.getFullName() +
                            "\nRole: " + currentUser.getRole() +
                            "\nSession time: " + new SimpleDateFormat("MM/dd/yyyy h:mm a").format(new Date())),
                    "Close Application", (Runnable) () -> {
                        int c = JOptionPane.showConfirmDialog(this, "Close the POS application?", "Close Application", JOptionPane.YES_NO_OPTION);
                        if (c == JOptionPane.YES_OPTION) System.exit(0);
                    },
                    "Minimize Application", (Runnable) () -> {
                        Window w = SwingUtilities.getWindowAncestor(this);
                        if (w instanceof Frame) ((Frame) w).setExtendedState(Frame.ICONIFIED);
                    },
                    "Activate Backup KDS", (Runnable) () -> {
                        operationsDAO.setSetting("kds_mode", "BACKUP");
                        info("Activate Backup KDS", "Kitchen Display System is now running on: BACKUP");
                    },
                    "Restore Primary KDS", (Runnable) () -> {
                        operationsDAO.setSetting("kds_mode", "PRIMARY");
                        info("Restore Primary KDS", "Kitchen Display System is now running on: PRIMARY");
                    }
                ));
                addFunctionRow(content, "Device Functions", UITheme.FUNC_DEVICE, actions(
                    "Upload Logo To Printer", (Runnable) () -> {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setDialogTitle("Select receipt printer logo image");
                        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                                "Image files", "png", "jpg", "jpeg", "gif", "bmp"));
                        int result = chooser.showOpenDialog(this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            String path = chooser.getSelectedFile().getAbsolutePath();
                            operationsDAO.setSetting("printer_logo_path", path);
                            info("Upload Logo To Printer", "Logo queued for the receipt printer:\n" + path);
                        }
                    }
                ));
                addFunctionRow(content, "Barcode Functions", UITheme.FUNC_BARCODE, actions(
                    "Barcode Entry", (Runnable) () -> {
                        String code = promptText("Scan or type a barcode:", "");
                        if (code == null || code.isEmpty()) return;
                        MenuItem found = menuItemDAO.findByBarcode(code);
                        if (found == null) { info("Barcode Entry", "No active menu item found for barcode: " + code); return; }
                        cart.add(new CartItem(found, 1));
                        refreshCartList();
                        info("Barcode Entry", "Added to order: " + found.getName() + " ($" + found.getPrice() + ")");
                    }
                ));
                addFunctionRow(content, "Drawer Functions", UITheme.FUNC_DRAWER, actions(
                    "Paid In", (Runnable) () -> doDrawerTxn("PAID_IN", "Paid In"),
                    "Paid Out", (Runnable) () -> doDrawerTxn("PAID_OUT", "Paid Out"),
                    "Cash Pull", (Runnable) () -> doDrawerTxn("CASH_PULL", "Cash Pull"),
                    "No Sale", (Runnable) () -> doDrawerTxn("NO_SALE", "No Sale")
                ));
                addFunctionRow(content, "Order Types", UITheme.FUNC_ORDERTYPE, actions(
                    "Change Order Type", (Runnable) () -> {
                        String[] types = {"DINE_IN", "PICKUP", "DELIVERY"};
                        String choice = (String) JOptionPane.showInputDialog(this, "New order type for the current order:",
                                "Change Order Type", JOptionPane.PLAIN_MESSAGE, null, types, orderTypeBox.getSelectedItem());
                        if (choice != null) orderTypeBox.setSelectedItem(choice);
                    }
                ));
                addFunctionRow(content, "Employee Functions", UITheme.FUNC_EMPLOYEE, actions(
                    "Assign Employee Id", (Runnable) () -> {
                        Employee emp = pickEmployee("Assign Employee Id");
                        if (emp == null) return;
                        actingEmployee = emp;
                        info("Assign Employee Id", "Acting employee for this session set to:\n" +
                                emp.getFullName() + " (ID " + emp.getId() + ", " + emp.getPosition() + ")");
                    },
                    "Employee Training", (Runnable) () -> {
                        Employee emp = pickEmployee("Employee Training — select employee");
                        if (emp == null) return;
                        String[] topics = {"POS Basics", "Food Safety", "Customer Service", "Cash Handling", "Allergen Awareness"};
                        String topic = (String) JOptionPane.showInputDialog(this, "Training topic completed by " + emp.getFullName() + ":",
                                "Employee Training", JOptionPane.PLAIN_MESSAGE, null, topics, topics[0]);
                        if (topic == null) return;
                        operationsDAO.recordTraining(emp.getId(), topic);
                        info("Employee Training", topic + " logged as completed for " + emp.getFullName() + ".");
                    }
                ));
                addFunctionRow(content, "Time Clock Function", UITheme.FUNC_TIMECLOCK, actions(
                    "Clock In/Out", (Runnable) () -> {
                        Employee emp = pickEmployee("Clock In/Out — select employee");
                        if (emp == null) return;
                        if (payrollDAO.getOpenEntry(emp.getId()) != null) {
                            payrollDAO.clockOut(emp.getId());
                            info("Clock In/Out", emp.getFullName() + " clocked OUT.");
                        } else {
                            payrollDAO.clockIn(emp.getId());
                            info("Clock In/Out", emp.getFullName() + " clocked IN.");
                        }
                    }
                ));
                addFunctionRow(content, "Check Functions", UITheme.FUNC_CHECK, actions(
                    "Adjust Closed Check From List", (Runnable) this::adjustClosedCheck,
                    "Adjust Closed Check", (Runnable) this::adjustClosedCheck,
                    "Transaction Return", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Transaction Return — pick completed order",
                                orderDAO.getOrdersByStatus("COMPLETED"));
                        if (order == null) return;
                        int orderId = (Integer) order[0];
                        BigDecimal orderTotal = (BigDecimal) order[5];
                        BigDecimal amount = promptAmount("Return amount (order total was $" + orderTotal + "):");
                        if (amount == null) return;
                        orderDAO.insertManualPayment(orderId, "CASH", amount.negate(), "RETURN");
                        info("Transaction Return", "Return of $" + amount + " recorded against order #" + orderId + ".");
                    },
                    "Menu Item Price Override", (Runnable) () -> {
                        MenuItem item = pickMenuItemBySearch("Menu Item Price Override");
                        if (item == null) return;
                        BigDecimal newPrice = promptAmount("New price for \"" + item.getName() + "\" (was $" + item.getPrice() + "):");
                        if (newPrice == null) return;
                        menuItemDAO.updatePrice(item.getId(), newPrice);
                        info("Menu Item Price Override", item.getName() + " price updated to $" + newPrice + ".");
                    },
                    "Manual Credit Entry", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Manual Credit Entry — pick order",
                                orderDAO.getOrdersByFilter(null, null, null));
                        if (order == null) return;
                        int orderId = (Integer) order[0];
                        BigDecimal amount = promptAmount("Manual credit amount:");
                        if (amount == null) return;
                        String[] methods = {"CASH", "CARD", "MOBILE_BANKING"};
                        String method = (String) JOptionPane.showInputDialog(this, "Payment method:", "Manual Credit Entry",
                                JOptionPane.PLAIN_MESSAGE, null, methods, methods[0]);
                        if (method == null) return;
                        orderDAO.insertManualPayment(orderId, method, amount, "MANUAL_CREDIT");
                        info("Manual Credit Entry", "$" + amount + " credited to order #" + orderId + " via " + method + ".");
                    },
                    "Cancel Saved/Stored Order", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Cancel Saved/Stored Order — pick open order", orderDAO.getOpenOrders());
                        if (order == null) return;
                        int orderId = (Integer) order[0];
                        int confirm = JOptionPane.showConfirmDialog(this, "Cancel order #" + orderId + "?", "Cancel Order", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            orderDAO.cancelOrder(orderId);
                            info("Cancel Saved/Stored Order", "Order #" + orderId + " cancelled.");
                        }
                    }
                ));
                addFunctionRow(content, "SmartSell Functions", UITheme.FUNC_SMARTSELL, actions(
                    "SmartSell On/Off", (Runnable) () -> doToggleSetting("smartsell_enabled", "SmartSell", "ON", "OFF"),
                    "SmartSell Leaderboard", (Runnable) () -> showReportDialog("SmartSell Leaderboard — Top Sellers (30 days)",
                            new String[]{"Item", "Category", "Qty Sold", "Revenue"}, operationsDAO.menuItemSalesReport())
                ));
                addFunctionRow(content, "Notification Center", UITheme.FUNC_NOTIFY, actions(
                    "Notification Center", (Runnable) () -> {
                        List<Object[]> rows = new ArrayList<>();
                        for (Object[] r : operationsDAO.lowStockIngredients())
                            rows.add(new Object[]{"LOW STOCK", r[0] + " (" + r[1] + " left, threshold " + r[2] + ")"});
                        for (Object[] n : operationsDAO.getNotifications())
                            rows.add(new Object[]{Boolean.TRUE.equals(n[2]) ? "read" : "NEW", n[1]});
                        showReportDialog("Notification Center", new String[]{"Status", "Message"}, rows);
                    },
                    "Consolidation Mode", (Runnable) () -> doToggleSetting("consolidation_mode", "Consolidation Mode", "ON", "OFF")
                ));
                addFunctionRow(content, "Menu Functions", UITheme.FUNC_MENU, actions(
                    "Menu Item Availability", (Runnable) () -> {
                        MenuItem item = pickMenuItemBySearch("Menu Item Availability");
                        if (item == null) return;
                        boolean newActive = !item.isActive();
                        menuItemDAO.setActive(item.getId(), newActive);
                        info("Menu Item Availability", item.getName() + " is now: " + (newActive ? "AVAILABLE" : "UNAVAILABLE"));
                    }
                ));
                addFunctionRow(content, "Phone Orders", UITheme.FUNC_PHONE, actions(
                    "Begin Phone Order", (Runnable) this::beginPhoneOrder
                ));
                addFunctionRow(content, "Gift Card Batch", UITheme.FUNC_GIFTCARD, actions(
                    "Navigator Batch Activation", (Runnable) () -> {
                        String s = promptText("How many gift cards to activate in this batch?", "10");
                        if (s == null) return;
                        int count;
                        try {
                            count = Integer.parseInt(s);
                            if (count <= 0) throw new NumberFormatException();
                        } catch (NumberFormatException ex) {
                            info("Navigator Batch Activation", "Enter a positive whole number.");
                            return;
                        }
                        operationsDAO.recordBatchOperation("GIFT_CARD_ACTIVATION", count, currentEmployeeId());
                        info("Navigator Batch Activation", "Batch of " + count + " gift cards activated and logged.");
                    }
                ));
                break;
            case "Daily Shift Functions":
                addFunctionBanner(content, "View Status", UITheme.FUNC_STATUS, (Runnable) () -> {
                    StringBuilder sb = new StringBuilder("Active till assignments:\n");
                    List<Object[]> assignments = operationsDAO.getActiveTillAssignments();
                    if (assignments.isEmpty()) sb.append("  (none)\n");
                    for (Object[] a : assignments) sb.append("  ").append(a[0]).append(" -> ").append(a[1])
                            .append(" (").append(a[2]).append(") since ").append(a[3]).append("\n");
                    sb.append("\nSafe balance: $").append(operationsDAO.currentSafeBalance());
                    sb.append("\nToday's Paid In: $").append(operationsDAO.sumTodayDrawerTransactions("PAID_IN"));
                    sb.append("\nToday's Paid Out: $").append(operationsDAO.sumTodayDrawerTransactions("PAID_OUT"));
                    sb.append("\nToday's Cash Pulls: $").append(operationsDAO.sumTodayDrawerTransactions("CASH_PULL"));
                    info("View Status", sb.toString());
                });
                addFunctionRow(content, "Till Management", UITheme.FUNC_TILL, actions(
                    "Assign Till to POS", (Runnable) () -> {
                        List<Object[]> free = operationsDAO.getUnassignedTills();
                        if (free.isEmpty()) { info("Assign Till to POS", "Every till is already assigned."); return; }
                        String[] labels = new String[free.size()];
                        for (int i = 0; i < free.size(); i++) labels[i] = free.get(i)[1].toString();
                        String choice = (String) JOptionPane.showInputDialog(this, "Till to assign:", "Assign Till to POS",
                                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
                        if (choice == null) return;
                        int tillId = (Integer) free.get(Arrays.asList(labels).indexOf(choice))[0];
                        Employee emp = pickEmployee("Assign to which employee?");
                        if (emp == null) return;
                        String register = promptText("Register name:", "Register 1");
                        operationsDAO.assignTill(tillId, emp.getId(), register);
                        info("Assign Till to POS", choice + " assigned to " + emp.getFullName() + " on " + register + ".");
                    },
                    "Assign User(s) to Till", (Runnable) () -> {
                        List<Object[]> tills = operationsDAO.getAllTills();
                        if (tills.isEmpty()) { info("Assign User(s) to Till", "No tills exist."); return; }
                        String[] labels = new String[tills.size()];
                        for (int i = 0; i < tills.size(); i++) labels[i] = tills.get(i)[1].toString();
                        String choice = (String) JOptionPane.showInputDialog(this, "Till:", "Assign User(s) to Till",
                                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
                        if (choice == null) return;
                        int tillId = (Integer) tills.get(Arrays.asList(labels).indexOf(choice))[0];
                        Employee emp = pickEmployee("Assign which employee to " + choice + "?");
                        if (emp == null) return;
                        String register = promptText("Register name:", "Register 1");
                        operationsDAO.assignTill(tillId, emp.getId(), register);
                        info("Assign User(s) to Till", emp.getFullName() + " assigned to " + choice + ".");
                    },
                    "Unassign Till from POS", (Runnable) () -> {
                        List<Object[]> assignments = operationsDAO.getActiveTillAssignments();
                        if (assignments.isEmpty()) { info("Unassign Till from POS", "No active till assignments."); return; }
                        String[] labels = new String[assignments.size()];
                        for (int i = 0; i < assignments.size(); i++) {
                            Object[] a = assignments.get(i);
                            labels[i] = a[0] + " - " + a[1] + " (" + a[2] + ")";
                        }
                        String choice = (String) JOptionPane.showInputDialog(this, "Assignment to end:", "Unassign Till from POS",
                                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
                        if (choice == null) return;
                        String tillName = assignments.get(Arrays.asList(labels).indexOf(choice))[0].toString();
                        Integer tillId = null;
                        for (Object[] t : operationsDAO.getAllTills()) if (t[1].equals(tillName)) tillId = (Integer) t[0];
                        operationsDAO.unassignTill(tillId, null);
                        info("Unassign Till from POS", choice + " unassigned.");
                    },
                    "Count Till", (Runnable) () -> {
                        List<Object[]> assignments = operationsDAO.getActiveTillAssignments();
                        if (assignments.isEmpty()) { info("Count Till", "No active till assignments to count."); return; }
                        String[] labels = new String[assignments.size()];
                        for (int i = 0; i < assignments.size(); i++) labels[i] = assignments.get(i)[0].toString();
                        String choice = (String) JOptionPane.showInputDialog(this, "Till to count:", "Count Till",
                                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
                        if (choice == null) return;
                        Integer tillId = null;
                        for (Object[] t : operationsDAO.getAllTills()) if (t[1].equals(choice)) tillId = (Integer) t[0];
                        BigDecimal counted = promptAmount("Counted cash amount:");
                        if (counted == null) return;
                        String notes = promptText("Notes (optional):", "");
                        operationsDAO.recordTillCount(tillId, counted, null, currentEmployeeId(), notes);
                        info("Count Till", choice + " counted: $" + counted);
                    }
                ));
                addFunctionRow(content, null, UITheme.FUNC_TILL_UNASSIGN, actions(
                    "Unassign User(s) from Till", (Runnable) () -> {
                        List<Object[]> assignments = operationsDAO.getActiveTillAssignments();
                        if (assignments.isEmpty()) { info("Unassign User(s) from Till", "No active till assignments."); return; }
                        String[] labels = new String[assignments.size()];
                        for (int i = 0; i < assignments.size(); i++) {
                            Object[] a = assignments.get(i);
                            labels[i] = a[0] + " - " + a[1];
                        }
                        String choice = (String) JOptionPane.showInputDialog(this, "Assignment to end:", "Unassign User(s) from Till",
                                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
                        if (choice == null) return;
                        Object[] picked = assignments.get(Arrays.asList(labels).indexOf(choice));
                        String tillName = picked[0].toString();
                        Integer tillId = null;
                        for (Object[] t : operationsDAO.getAllTills()) if (t[1].equals(tillName)) tillId = (Integer) t[0];
                        Employee emp = null;
                        for (Employee e : employeeDAO.getAllEmployees()) if (e.getFullName().equals(picked[1])) emp = e;
                        operationsDAO.unassignTill(tillId, emp != null ? emp.getId() : null);
                        info("Unassign User(s) from Till", choice + " ended.");
                    }
                ));
                addFunctionBanner(content, "Cash Management Dashboard", UITheme.FUNC_CASHDASH, (Runnable) () -> {
                    List<Object[]> rows = new ArrayList<>();
                    rows.add(new Object[]{"Safe Balance", "$" + operationsDAO.currentSafeBalance()});
                    rows.add(new Object[]{"Today's Paid In", "$" + operationsDAO.sumTodayDrawerTransactions("PAID_IN")});
                    rows.add(new Object[]{"Today's Paid Out", "$" + operationsDAO.sumTodayDrawerTransactions("PAID_OUT")});
                    rows.add(new Object[]{"Today's Cash Pulls", "$" + operationsDAO.sumTodayDrawerTransactions("CASH_PULL")});
                    rows.add(new Object[]{"Active Till Assignments", operationsDAO.getActiveTillAssignments().size()});
                    showReportDialog("Cash Management Dashboard", new String[]{"Metric", "Value"}, rows);
                });
                addFunctionRow(content, "Safe / Cash Pull", UITheme.FUNC_SAFE, actions(
                    "Open Safe or Cash Pull", (Runnable) () -> doSafeTxn("OPEN", "Open Safe"),
                    "Add Funds", (Runnable) () -> doSafeTxn("ADD_FUNDS", "Add Funds"),
                    "Count Safe or Cash Pull", (Runnable) () -> {
                        BigDecimal counted = promptAmount("Counted safe amount:");
                        if (counted == null) return;
                        String notes = promptText("Notes (optional):", "");
                        operationsDAO.recordSafeTransaction("COUNT", counted, currentEmployeeId(), notes);
                        BigDecimal expected = operationsDAO.currentSafeBalance();
                        info("Count Safe or Cash Pull", "Counted: $" + counted + "\nExpected (ledger balance): $" + expected +
                                "\nVariance: $" + counted.subtract(expected));
                    },
                    "Close Safe or Cash Pull", (Runnable) () -> doSafeTxn("CLOSE", "Close Safe"),
                    "Deposit Cash from Safe or Cash Pull", (Runnable) () -> doSafeTxn("DEPOSIT", "Bank Deposit")
                ));
                addFunctionRow(content, "Reports", UITheme.FUNC_SHIFTREPORT, actions(
                    "Cash Drawer Report", (Runnable) () -> showReportDialog("Cash Drawer Report (Today)",
                            new String[]{"Type", "Amount", "Reason", "Employee", "Time"}, operationsDAO.getTodayDrawerTransactions()),
                    "Over/Short Report", (Runnable) () -> showReportDialog("Over/Short Report (Till Counts)",
                            new String[]{"Till", "Counted", "Expected", "Variance", "Counted By", "Time"}, operationsDAO.getTillCountHistory()),
                    "Paid-in/Paid Out", (Runnable) () -> {
                        List<Object[]> rows = new ArrayList<>();
                        for (Object[] r : operationsDAO.getAllDrawerTransactions())
                            if ("PAID_IN".equals(r[0]) || "PAID_OUT".equals(r[0])) rows.add(r);
                        showReportDialog("Paid-in / Paid Out", new String[]{"Type", "Amount", "Reason", "Employee", "Time"}, rows);
                    },
                    "Safe/Cash Pull Report", (Runnable) () -> showReportDialog("Safe / Cash Pull Report",
                            new String[]{"Type", "Amount", "Notes", "Employee", "Time"}, operationsDAO.getAllSafeTransactions()),
                    "Bank Deposits Report", (Runnable) () -> {
                        List<Object[]> rows = new ArrayList<>();
                        for (Object[] r : operationsDAO.getAllSafeTransactions()) if ("DEPOSIT".equals(r[0])) rows.add(r);
                        showReportDialog("Bank Deposits Report", new String[]{"Type", "Amount", "Notes", "Employee", "Time"}, rows);
                    }
                ));
                break;
            case "Reports":
                addFunctionBanner(content, "All Reports", UITheme.FUNC_ALLREPORTS, (Runnable) () ->
                        showReportDialog("Property Financial (30 days)", new String[]{"Metric", "Value"}, operationsDAO.propertyFinancialReport()));
                addFunctionRow(content, "Financial Reports", UITheme.FUNC_ALLREPORTS, actions(
                    "Employee Financial", (Runnable) () -> showReportDialog("Employee Financial",
                            new String[]{"Employee", "Position", "Payroll Runs", "Total Gross", "Total Net"}, operationsDAO.employeeFinancialReport()),
                    "Property Financial", (Runnable) () -> showReportDialog("Property Financial (30 days)",
                            new String[]{"Metric", "Value"}, operationsDAO.propertyFinancialReport()),
                    "Menu Item Summary", (Runnable) () -> showReportDialog("Menu Item Summary (30 days, by revenue)",
                            new String[]{"Item", "Category", "Qty Sold", "Revenue"}, operationsDAO.menuItemSummaryReport())
                ));
                addFunctionRow(content, "Sales Reports", UITheme.FUNC_ALLREPORTS, actions(
                    "Menu Item Sales", (Runnable) () -> showReportDialog("Menu Item Sales (30 days, by units)",
                            new String[]{"Item", "Category", "Qty Sold", "Revenue"}, operationsDAO.menuItemSalesReport()),
                    "Family Group Sales", (Runnable) () -> showReportDialog("Family Group Sales (30 days, by department)",
                            new String[]{"Department", "Qty Sold", "Revenue"}, operationsDAO.familyGroupSalesReport()),
                    "Major Group Sales", (Runnable) () -> showReportDialog("Major Group Sales (30 days, by subcategory)",
                            new String[]{"Category", "Qty Sold", "Revenue"}, operationsDAO.majorGroupSalesReport())
                ));
                addFunctionRow(content, "Labor Reports", UITheme.FUNC_ALLREPORTS, actions(
                    "Clock-in Status", (Runnable) () -> showReportDialog("Clock-in Status (currently clocked in)",
                            new String[]{"Employee", "Clock In"}, operationsDAO.clockInStatusReport()),
                    "Employee Labor Summary", (Runnable) () -> showReportDialog("Employee Labor Summary (30 days)",
                            new String[]{"Employee", "Total Hours"}, operationsDAO.employeeLaborSummaryReport()),
                    "Time Period Summary", (Runnable) () -> showReportDialog("Time Period Summary (14 days)",
                            new String[]{"Date", "Total Hours"}, operationsDAO.timePeriodSummaryReport())
                ));
                addFunctionRow(content, "Check Reports", UITheme.FUNC_ALLREPORTS, actions(
                    "Employee Closed Check", (Runnable) () -> showReportDialog("Employee Closed Check",
                            new String[]{"Order #", "Cashier", "Total", "Placed"}, operationsDAO.employeeClosedCheckReport()),
                    "Employee Open Check", (Runnable) () -> showReportDialog("Employee Open Check",
                            new String[]{"Order #", "Cashier", "Total", "Placed"}, operationsDAO.employeeOpenCheckReport())
                ));
                break;
            case "All Open Checks":
                addFunctionRow(content, "All Open Checks", UITheme.FUNC_OPENCHECKS, actions(
                    "View All Open Checks", (Runnable) () -> showReportDialog("All Open Checks",
                            new String[]{"Order #", "Source", "Type", "Customer", "Cashier", "Total", "Placed"}, orderDAO.getOpenOrders()),
                    "Filter by Table", (Runnable) () -> {
                        info("Filter by Table", "This POS doesn't track dine-in table numbers, so table filtering isn't applicable — showing all open checks instead.");
                        showReportDialog("All Open Checks", new String[]{"Order #", "Source", "Type", "Customer", "Cashier", "Total", "Placed"}, orderDAO.getOpenOrders());
                    },
                    "Filter by Server", (Runnable) () -> showReportDialog("Open Checks — " + currentUser.getFullName(),
                            new String[]{"Order #", "Source", "Type", "Customer", "Cashier", "Total", "Placed"}, orderDAO.getOpenOrdersByServer(currentUser.getId())),
                    "Merge Checks", (Runnable) () -> {
                        List<Object[]> open = orderDAO.getOpenOrders();
                        Object[] from = pickOrderFromList("Merge Checks — merge FROM this order", open);
                        if (from == null) return;
                        Object[] into = pickOrderFromList("Merge Checks — INTO this order", open);
                        if (into == null) return;
                        if (from[0].equals(into[0])) { info("Merge Checks", "Pick two different orders."); return; }
                        orderDAO.mergeOrders((Integer) from[0], (Integer) into[0]);
                        info("Merge Checks", "Order #" + from[0] + " merged into #" + into[0] + ".");
                    }
                ));
                break;
            case "Phone Orders":
                addFunctionRow(content, "Phone Orders", UITheme.FUNC_PHONE, actions(
                    "View Phone Order Queue", (Runnable) () -> showReportDialog("Phone Order Queue",
                            new String[]{"Order #", "Source", "Type", "Customer", "Cashier", "Total", "Placed"},
                            orderDAO.getOrdersByFilter("OPEN", "PHONE", null)),
                    "Begin Phone Order", (Runnable) this::beginPhoneOrder,
                    "Cancel Phone Order", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Cancel Phone Order", orderDAO.getOrdersByFilter("OPEN", "PHONE", null));
                        if (order == null) return;
                        orderDAO.cancelOrder((Integer) order[0]);
                        info("Cancel Phone Order", "Order #" + order[0] + " cancelled.");
                    }
                ));
                break;
            case "DT Orders":
                addFunctionRow(content, "DT Orders", UITheme.FUNC_DT, actions(
                    "View DT Queue", (Runnable) () -> showReportDialog("DT (Drive-Thru) Queue",
                            new String[]{"Order #", "Source", "Type", "Customer", "Cashier", "Total", "Placed"},
                            orderDAO.getOrdersByFilter("OPEN", "IN_STORE", "PICKUP")),
                    "Recall DT Order", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Recall DT Order", orderDAO.getOrdersByFilter("OPEN", "IN_STORE", "PICKUP"));
                        if (order == null) return;
                        int orderId = (Integer) order[0];
                        operationsDAO.addOrderFlag(orderId, "RECALLED");
                        StringBuilder sb = new StringBuilder("Order #" + orderId + " (" + order[3] + ", $" + order[5] + ")\n\n");
                        for (Object[] item : orderDAO.getOrderItems(orderId))
                            sb.append(item[1]).append(" x ").append(item[0]).append("  $").append(item[3]).append("\n");
                        info("Recall DT Order", sb.toString());
                    },
                    "Reset DT Timer", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Reset DT Timer", orderDAO.getOrdersByFilter("OPEN", "IN_STORE", "PICKUP"));
                        if (order == null) return;
                        operationsDAO.addOrderFlag((Integer) order[0], "RECALLED");
                        info("Reset DT Timer", "Timer reset logged for order #" + order[0] + " at " +
                                new SimpleDateFormat("h:mm:ss a").format(new Date()) + ".");
                    }
                ));
                break;
            case "OTG Orders":
                addFunctionRow(content, "OTG Orders", UITheme.FUNC_OTG, actions(
                    "View OTG Queue", (Runnable) () -> showReportDialog("OTG (Online Pickup) Queue",
                            new String[]{"Order #", "Source", "Type", "Customer", "Cashier", "Total", "Placed"},
                            orderDAO.getOrdersByFilter("OPEN", "ONLINE", "PICKUP")),
                    "Mark OTG Ready", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Mark OTG Ready", orderDAO.getOrdersByFilter("OPEN", "ONLINE", "PICKUP"));
                        if (order == null) return;
                        operationsDAO.addOrderFlag((Integer) order[0], "READY");
                        info("Mark OTG Ready", "Order #" + order[0] + " marked ready for pickup.");
                    },
                    "Print OTG Ticket", (Runnable) () -> {
                        Object[] order = pickOrderFromList("Print OTG Ticket", orderDAO.getOrdersByFilter(null, "ONLINE", "PICKUP"));
                        if (order == null) return;
                        int orderId = (Integer) order[0];
                        StringBuilder ticket = new StringBuilder();
                        ticket.append("      NY COFFEE CO.\n");
                        ticket.append("  Order #").append(orderId).append("   ").append(order[6]).append("\n");
                        ticket.append("  Customer: ").append(order[3]).append("\n");
                        ticket.append("  --------------------------\n");
                        for (Object[] item : orderDAO.getOrderItems(orderId))
                            ticket.append("  ").append(item[1]).append(" x ").append(item[0])
                                  .append("   $").append(item[3]).append("\n");
                        ticket.append("  --------------------------\n");
                        ticket.append("  TOTAL: $").append(order[5]).append("\n");
                        JTextArea area = new JTextArea(ticket.toString());
                        area.setEditable(false);
                        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
                        JOptionPane.showMessageDialog(this, new JScrollPane(area), "OTG Ticket — Order #" + orderId, JOptionPane.PLAIN_MESSAGE);
                    }
                ));
                break;
            case "Order Confirm Board":
                addFunctionBanner(content, "Order Confirmation Board Display Functions", UITheme.FUNC_CONFIRM_BANNER, (Runnable) this::showDisplayStatus);
                addFunctionRowMixed(content, null, actions(
                        "Activate Display A", (Runnable) () -> {
                            if (displayA == null) {
                                displayA = new OrderConfirmationBoardFrame("Display A");
                                displayA.setLocation(80, 80);
                                displayA.addWindowListener(new java.awt.event.WindowAdapter() {
                                    @Override public void windowClosed(java.awt.event.WindowEvent e) { displayA = null; }
                                });
                            }
                            displayA.setVisible(true);
                            displayA.toFront();
                            info("Activate Display A", "Display A is now ACTIVE.");
                        },
                        "Deactivate The Display", (Runnable) () -> {
                            boolean any = false;
                            if (displayA != null) { displayA.dispose(); displayA = null; any = true; }
                            if (displayB != null) { displayB.dispose(); displayB = null; any = true; }
                            info("Deactivate The Display", any ? "Display(s) deactivated." : "No display was active.");
                        }
                    ), new Color[]{UITheme.FUNC_CONFIRM_GREEN, UITheme.FUNC_CONFIRM_RED});
                addFunctionRowMixed(content, null, actions(
                        "Activate Display B", (Runnable) () -> {
                            if (displayB == null) {
                                displayB = new OrderConfirmationBoardFrame("Display B");
                                displayB.setLocation(820, 80);
                                displayB.addWindowListener(new java.awt.event.WindowAdapter() {
                                    @Override public void windowClosed(java.awt.event.WindowEvent e) { displayB = null; }
                                });
                            }
                            displayB.setVisible(true);
                            displayB.toFront();
                            info("Activate Display B", "Display B is now ACTIVE.");
                        }
                    ), new Color[]{UITheme.FUNC_CONFIRM_GREEN});
                addFunctionBanner(content, "Order Confirmation Board Support Functions", UITheme.FUNC_CONFIRM_BANNER, (Runnable) this::showDisplayStatus);
                addFunctionRow(content, null, UITheme.FUNC_APP, actions(
                    "View Display Status", (Runnable) this::showDisplayStatus,
                    "Test Display", (Runnable) () -> {
                        if (displayA == null && displayB == null) { info("Test Display", "No display is active — activate Display A or B first."); return; }
                        if (displayA != null) displayA.flashTestSignal();
                        if (displayB != null) displayB.flashTestSignal();
                        info("Test Display", "Test signal sent to active display(s).");
                    }
                ));
                break;
            case "SUPPORT":
                addFunctionRow(content, "SUPPORT", UITheme.FUNC_SUPPORT, actions(
                    "Call Support", (Runnable) () -> info("Call Support",
                            "NY Coffee Co. POS Support\nPhone: 1-800-555-0199\nAvailable 24/7 for register and payment issues."),
                    "Chat with Support", (Runnable) () -> info("Chat with Support",
                            "Live chat isn't available in this offline build.\nPlease call 1-800-555-0199 or use Knowledge Base for self-serve help."),
                    "Knowledge Base", (Runnable) () -> {
                        Window w = SwingUtilities.getWindowAncestor(this);
                        new HelpDialog(w instanceof Frame ? (Frame) w : null).setVisible(true);
                    },
                    "Restart POS Terminal", (Runnable) () -> {
                        int confirm = JOptionPane.showConfirmDialog(this, "Restart the POS terminal application now?",
                                "Restart POS Terminal", JOptionPane.YES_NO_OPTION);
                        if (confirm != JOptionPane.YES_OPTION) return;
                        try {
                            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                            String classpath = System.getProperty("java.class.path");
                            new ProcessBuilder(javaBin, "-cp", classpath, "com.possystem.Main").start();
                            System.exit(0);
                        } catch (java.io.IOException ex) {
                            info("Restart POS Terminal", "Could not relaunch automatically: " + ex.getMessage() + "\nPlease restart the application manually.");
                        }
                    }
                ));
                break;
            default:
                JLabel empty = new JLabel(I18n.t(groupName));
                empty.setForeground(UITheme.SECTION_HEADER_COLOR);
                content.add(empty);
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        gridContainer.removeAll();
        gridContainer.add(scroll, BorderLayout.CENTER);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    // ---------- SUBCATEGORY SIDEBAR (EAST) ----------
    private void rebuildSidebar(List<Category> children) {
        sidebarButtons.clear();
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        if (children != null) {
            for (Category c : children) {
                JButton b = UITheme.styledButton(c.getName(), UITheme.SIDEBAR_ITEM_BG);
                b.setForeground(UITheme.DEPT_TAB_TEXT);
                b.setAlignmentX(Component.CENTER_ALIGNMENT);
                b.setMaximumSize(new Dimension(150, 56));
                b.addActionListener(e -> selectSubcategory(c));
                sidebarButtons.put(c.getName(), b);
                inner.add(b);
                inner.add(Box.createVerticalStrut(6));
            }
        }
        // "News & Promos" is a client-side info board for the Featured department, not a
        // real purchasable subcategory, so it isn't backed by a `categories` row.
        if (currentDepartment != null && "Featured".equals(currentDepartment.getName())) {
            JButton newsBtn = UITheme.styledButton(I18n.t("News & Promos"), UITheme.SIDEBAR_ITEM_BG);
            newsBtn.setForeground(UITheme.DEPT_TAB_TEXT);
            newsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            newsBtn.setMaximumSize(new Dimension(150, 56));
            newsBtn.addActionListener(e -> showFeaturedNews());
            sidebarButtons.put("News & Promos", newsBtn);
            inner.add(newsBtn);
            inner.add(Box.createVerticalStrut(6));
        }
        inner.add(Box.createVerticalGlue());

        JPanel bg = UITheme.gradientPanel(UITheme.SIDEBAR_BG, new Color(250, 226, 196));
        bg.add(inner, BorderLayout.CENTER);

        sidebarContainer.removeAll();
        sidebarContainer.add(bg, BorderLayout.CENTER);
        sidebarContainer.revalidate();
        sidebarContainer.repaint();
    }

    private void highlightSidebarItem(String activeName) {
        for (var entry : sidebarButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeName);
            entry.getValue().setBackground(active ? UITheme.SIDEBAR_ACTIVE : UITheme.SIDEBAR_ITEM_BG);
            entry.getValue().setForeground(active ? Color.WHITE : UITheme.DEPT_TAB_TEXT);
        }
    }

    // ---------- CART COLUMN (WEST) ----------
    private JComponent buildCartColumn() {
        JPanel column = new JPanel(new BorderLayout());
        column.setPreferredSize(new Dimension(300, 0));
        column.setBackground(UITheme.CART_BG);

        cartListPanel.setLayout(new BoxLayout(cartListPanel, BoxLayout.Y_AXIS));
        cartListPanel.setBackground(UITheme.CART_BG);
        JScrollPane scroll = new JScrollPane(cartListPanel);
        scroll.setBorder(null);
        column.add(scroll, BorderLayout.CENTER);

        column.add(buildTotalsAndActions(), BorderLayout.SOUTH);
        return column;
    }

    private JComponent buildTotalsAndActions() {
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBackground(UITheme.CART_BG);
        south.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        orderTypeLabel.setText(I18n.t("Order Type:"));
        sourceLabel.setText(I18n.t("Source:"));
        discountLabel.setText(I18n.t("Discount $:"));

        JPanel orderRow = new JPanel(new GridLayout(2, 2, 4, 2));
        orderRow.setOpaque(false);
        orderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderRow.setMaximumSize(new Dimension(280, 50));
        orderRow.add(orderTypeLabel);
        orderRow.add(orderTypeBox);
        orderRow.add(sourceLabel);
        orderRow.add(orderSourceBox);
        orderTypeBox.addActionListener(e -> onOrderTypeChanged());
        south.add(orderRow);
        south.add(Box.createVerticalStrut(4));

        JPanel discountRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        discountRow.setOpaque(false);
        discountRow.add(discountLabel);
        discountField.addActionListener(e -> recalcTotals());
        discountRow.add(discountField);
        south.add(discountRow);

        loyaltyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loyaltyBtn.setMaximumSize(new Dimension(280, 34));
        loyaltyBtn.addActionListener(e -> openLoyaltyDialog());
        south.add(loyaltyBtn);
        south.add(Box.createVerticalStrut(4));

        for (JLabel l : new JLabel[]{subtotalLabel, taxLabel}) {
            l.setFont(l.getFont().deriveFont(Font.PLAIN, 13f));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.add(l);
        }
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(totalLabel);

        south.add(Box.createVerticalStrut(8));

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 4, 4));
        actionRow.setOpaque(false);
        removeBtn.addActionListener(e -> {
            if (!cart.isEmpty()) { cart.remove(cart.size() - 1); refreshCartList(); }
        });
        cancelBtn.addActionListener(e -> { cart.clear(); discountField.setText("0.00"); refreshCartList(); });
        actionRow.add(removeBtn);
        actionRow.add(cancelBtn);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(280, 36));
        south.add(actionRow);

        south.add(Box.createVerticalStrut(6));

        JPanel cashRow = new JPanel(new GridLayout(1, 3, 4, 4));
        cashRow.setOpaque(false);
        cashRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cashRow.setMaximumSize(new Dimension(280, 34));
        for (String amt : new String[]{"$5", "$10", "$20"}) {
            JButton b = UITheme.styledButton(amt, UITheme.CASH_GREEN);
            cashRow.add(b);
        }
        south.add(cashRow);

        south.add(Box.createVerticalStrut(6));

        payBtn.setFont(payBtn.getFont().deriveFont(18f));
        payBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        payBtn.setMaximumSize(new Dimension(280, 44));
        payBtn.addActionListener(e -> openPaymentsScreen());
        south.add(payBtn);

        return south;
    }

    // ---------- ITEM GRID (CENTER, "GRID") ----------
    // Warm gradient background behind the item grid, per the "nice colour of background" request.
    private final JPanel gridContainer = UITheme.gradientPanel(UITheme.GRID_GRADIENT_TOP, UITheme.GRID_GRADIENT_BOTTOM);

    /** Renders the active items of one category, grouped under `section` header labels
     *  (e.g. Coffee -> "Blend" / "Cold Brew"), matching the reference terminal's item grid. */
    private void showItemsForCategory(Category cat) {
        List<MenuItem> items = menuItemDAO.getMenuItemsByCategory(cat.getId());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        if (items.isEmpty()) {
            JLabel empty = new JLabel("No items yet in " + cat.getName() + ".");
            empty.setForeground(UITheme.SECTION_HEADER_COLOR);
            empty.setFont(empty.getFont().deriveFont(Font.BOLD, 16f));
            content.add(empty);
        } else {
            java.util.LinkedHashMap<String, List<MenuItem>> bySection = new java.util.LinkedHashMap<>();
            for (MenuItem item : items) {
                String section = item.getSection() == null ? cat.getName() : item.getSection();
                bySection.computeIfAbsent(section, s -> new ArrayList<>()).add(item);
            }
            for (var entry : bySection.entrySet()) {
                JLabel header = new JLabel(entry.getKey());
                header.setForeground(UITheme.SECTION_HEADER_COLOR);
                header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                header.setBorder(BorderFactory.createEmptyBorder(10, 2, 6, 0));
                content.add(header);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
                row.setOpaque(false);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                String deptName = currentDepartment != null ? currentDepartment.getName() : "";
                boolean featured = "Featured".equals(deptName);
                boolean bakery = "Bakery".equals(deptName);
                boolean sandwiches = "Sandwiches".equals(deptName);
                boolean retail = "Retail".equals(deptName);
                boolean local = "Local".equals(deptName);
                boolean beverages = "Beverages".equals(deptName);
                for (MenuItem item : entry.getValue()) {
                    String label = "<html><center>" + item.getName() + "<br>$" + item.getPrice() + "</center></html>";
                    JButton btn;
                    if (featured || bakery || sandwiches || retail || local || beverages) {
                        // Flat colored button via the same helper the tabs/PAY button use — on
                        // this look-and-feel, setBackground() is ignored when borderPainted is
                        // true, so styledButton (borderPainted=false) is what actually shows color.
                        Color c = featured ? colorForFeaturedItem(item.getName())
                                : bakery ? colorForBakeryItem(item.getName())
                                : sandwiches ? colorForSandwichItem(item.getName())
                                : retail ? colorForRetailItem(item.getName())
                                : beverages ? colorForBeverageItem(entry.getKey())
                                : colorForLocalItem(item.getName());
                        btn = UITheme.styledButton(label, c);
                    } else {
                        btn = new JButton(label);
                        btn.setOpaque(true);
                        btn.setBorderPainted(true);
                        btn.setBackground(UITheme.ITEM_CARD_BG);
                        btn.setBorder(BorderFactory.createLineBorder(UITheme.ITEM_CARD_BORDER, 1));
                    }
                    btn.setPreferredSize(new Dimension(150, 90));
                    btn.addActionListener(e -> openCustomize(item));
                    row.add(btn);
                }
                content.add(row);
            }
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        gridContainer.removeAll();
        gridContainer.add(scroll, BorderLayout.CENTER);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    /** Color-codes Beverages tab buttons by section (Blend/Cold Brew/Iced Coffee/... within
     *  Coffee, Single/Double Espresso/... within Espresso, and so on for Teas/Refreshers/
     *  Frozen/Other Beverages), since every beverage item already carries a clean section
     *  label — far more reliable than guessing from the item name. */
    private Color colorForBeverageItem(String section) {
        if (section == null) return UITheme.BEV_DEFAULT;
        switch (section) {
            case "Blend": return UITheme.BEV_COFFEE_BLEND;
            case "Cold Brew": return UITheme.BEV_COFFEE_COLDBREW;
            case "Iced Coffee": return UITheme.BEV_COFFEE_ICED;
            case "Flavored Hot Coffee": return UITheme.BEV_COFFEE_HOTFLAVORED;
            case "Decaf & Light Roast": return UITheme.BEV_COFFEE_DECAF;
            case "Single Espresso": return UITheme.BEV_ESP_SINGLE;
            case "Double Espresso": return UITheme.BEV_ESP_DOUBLE;
            case "Alternative Beverages": return UITheme.BEV_ESP_ALT;
            case "Seasonal Espresso": return UITheme.BEV_ESP_SEASONAL;
            case "Iced Tea": return UITheme.BEV_TEA_ICED;
            case "Hot Tea": return UITheme.BEV_TEA_HOT;
            case "Chai & Matcha": return UITheme.BEV_TEA_CHAI_MATCHA;
            case "Bubble & Specialty Tea": return UITheme.BEV_TEA_BUBBLE;
            case "Classic": return UITheme.BEV_REF_CLASSIC;
            case "Daydream": return UITheme.BEV_REF_DAYDREAM;
            case "Energy Refreshers": return UITheme.BEV_REF_ENERGY;
            case "Fruit Splash": return UITheme.BEV_REF_SPLASH;
            case "Mix It": return UITheme.BEV_REF_MIXIT;
            case "Sparkling Refreshers": return UITheme.BEV_REF_SPARKLING;
            case "Frozen Coolers": return UITheme.BEV_FROZEN_COOLERS;
            case "Frozen Coffee": return UITheme.BEV_FROZEN_COFFEE;
            case "Frozen Specialty": return UITheme.BEV_FROZEN_SPECIALTY;
            case "Other Frozen": return UITheme.BEV_FROZEN_OTHER;
            case "Frozen Fruit Blends": return UITheme.BEV_FROZEN_FRUIT;
            case "Coolers & Fountain": return UITheme.BEV_OTHER_FOUNTAIN;
            case "Zero Sugar": return UITheme.BEV_OTHER_ZERO;
            case "Bulk": return UITheme.BEV_OTHER_BULK;
            case "Bottled & Juices": return UITheme.BEV_OTHER_BOTTLED;
            case "Milk & Extras": return UITheme.BEV_OTHER_MILK;
            case "Kids Drinks": return UITheme.BEV_OTHER_KIDS;
            default: return UITheme.BEV_DEFAULT;
        }
    }

    /** Color-codes Featured tab buttons by drink type (refresher/latte/matcha/...), same
     *  visual pattern as a typical counter-service Featured board. */
    private Color colorForFeaturedItem(String name) {
        String n = name.toLowerCase();
        if (n.contains("bundle")) return UITheme.FEATURED_BUNDLE;
        if (n.contains("chiller")) return UITheme.FEATURED_CHILLER;
        if (n.contains("zero")) return UITheme.FEATURED_ZERO;
        if (n.contains("lemonade") || n.contains("limeade")) return UITheme.FEATURED_LEMONADE;
        if (n.contains("matcha")) return UITheme.FEATURED_MATCHA;
        if (n.contains("latte") || n.contains("espresso") || n.contains("coffee")) return UITheme.FEATURED_LATTE;
        if (n.contains("refresher") || n.contains("spritz") || n.contains("daydream")) return UITheme.FEATURED_REFRESHER;
        return UITheme.FEATURED_DEFAULT;
    }

    /** Color-codes Bakery tab buttons by treat type (donut/donut hole/fancy pastry/bagel/
     *  muffin/cookie/snack...), same visual pattern as a typical counter-service bakery board. */
    private Color colorForBakeryItem(String name) {
        String n = name.toLowerCase();
        if (n.contains("donut hole")) return UITheme.BAKERY_DONUT_HOLE;
        if (n.contains("fritter") || n.contains("long john") || n.contains("eclair")
            || n.contains("coffee roll") || n.contains("cruller") || n.contains("twist")) return UITheme.BAKERY_FANCY;
        if (n.contains("donut")) return UITheme.BAKERY_DONUT;
        if (n.contains("bagel")) return UITheme.BAKERY_BAGEL;
        if (n.contains("muffin")) return UITheme.BAKERY_MUFFIN;
        if (n.contains("cream cheese") || n.contains("butter")) return UITheme.BAKERY_SPREAD;
        if (n.contains("cookie") || n.contains("snickerdoodle")) return UITheme.BAKERY_COOKIE;
        if (n.contains("danish") || n.contains("croissant") || n.contains("cinnamon roll") || n.contains("cake pop")) return UITheme.BAKERY_PASTRY;
        if (n.contains("brownie") || n.contains("blondie") || n.contains("bar") || n.contains("cake")) return UITheme.BAKERY_TREAT;
        return UITheme.BAKERY_SNACK;
    }

    /** Color-codes Sandwiches tab buttons by item type (breakfast/melt/deli/wrap/bowl/
     *  hash brown/bite/side/snack...), same visual pattern as a typical counter-service
     *  sandwich board. */
    private Color colorForSandwichItem(String name) {
        String n = name.toLowerCase();
        if (n.contains("wrap")) return UITheme.SANDWICH_WRAP;
        if (n.contains("bowl")) return UITheme.SANDWICH_BOWL;
        if (n.contains("hash brown")) return UITheme.SANDWICH_HASHBROWN;
        if (n.contains("bite")) return UITheme.SANDWICH_BITE;
        if (n.contains("side of")) return UITheme.SANDWICH_SIDE;
        if (n.contains("grilled") || n.contains("melt")) return UITheme.SANDWICH_MELT;
        if (n.contains("egg") || n.contains("breakfast") || n.contains("omelet")) return UITheme.SANDWICH_BREAKFAST;
        if (n.contains("mozzarella") || n.contains("onion ring") || n.contains("fries") || n.contains("pretzel")
            || n.contains("tender") || n.contains("curd") || n.contains("popcorn")) return UITheme.SANDWICH_SNACK;
        if (n.contains("sandwich") || n.contains("club")) return UITheme.SANDWICH_DELI;
        return UITheme.SANDWICH_DEFAULT;
    }

    private Color colorForRetailItem(String name) {
        String n = name.toLowerCase();
        if (n.contains("gift card")) return UITheme.RETAIL_CARD;
        if (n.contains("k-cup")) return UITheme.RETAIL_KCUP;
        if (n.contains("tea")) return UITheme.RETAIL_TEA;
        if (n.contains("brewer")) return UITheme.RETAIL_BREWER;
        if (n.contains("tumbler") || n.contains("stainless") || n.contains("water bottle") || n.contains("cold cup")
            || n.contains("carafe") || n.contains("cup") || n.contains("plastic bottle")) return UITheme.RETAIL_DRINKWARE;
        if (n.contains("mug")) return UITheme.RETAIL_MUG;
        if (n.contains("holiday")) return UITheme.RETAIL_HOLIDAY;
        if (n.contains("gift basket") || n.contains("gift set") || n.contains("gift box") || n.contains("gift bag")) return UITheme.RETAIL_GIFT;
        if (n.contains("bottled") || n.contains("sports drink") || n.contains("energy drink") || n.contains("juice")) return UITheme.RETAIL_BOTTLED;
        if (n.contains("chocolate") || n.contains("gummy") || n.contains("candy") || n.contains("gum") || n.contains("caramel chews")
            || n.contains("mint tin") || n.contains("licorice") || n.contains("toffee") || n.contains("chews")) return UITheme.RETAIL_CANDY;
        if (n.contains("trail mix") || n.contains("pretzel") || n.contains("chips") || n.contains("popcorn") || n.contains("granola")
            || n.contains("fruit snack") || n.contains("cracker") || n.contains("jerky") || n.contains("rice cake") || n.contains("nut mix")
            || n.contains("dried fruit") || n.contains("sesame") || n.contains("bar") || n.contains("cookie") || n.contains("muffin")
            || n.contains("parfait") || n.contains("oats") || n.contains("puffs")) return UITheme.RETAIL_SNACK;
        if (n.contains("t-shirt") || n.contains("hoodie") || n.contains("baseball cap") || n.contains("beanie") || n.contains("apron")
            || n.contains("pin set") || n.contains("sticker") || n.contains("keychain") || n.contains("tote") || n.contains("notebook")) return UITheme.RETAIL_MERCH;
        if (n.contains("scoop") || n.contains("frother") || n.contains("strap") || n.contains("sleeve") || n.contains("spoon")
            || n.contains("scale") || n.contains("tray") || n.contains("brush") || n.contains("canister") || n.contains("lid")
            || n.contains("coaster") || n.contains("grinder") || n.contains("tamper") || n.contains("filter") || n.contains("straw")) return UITheme.RETAIL_ACCESSORY;
        if (n.contains("coffee") || n.contains("espresso")) return UITheme.RETAIL_COFFEE;
        return UITheme.RETAIL_DEFAULT;
    }

    private Color colorForLocalItem(String name) {
        String n = name.toLowerCase();
        if (n.contains("donation")) return UITheme.LOCAL_DONATION;
        if (n.contains("bag deposit") || n.contains("bag fee")) return UITheme.LOCAL_BAGFEE;
        if (n.contains("pup cup") || n.contains("dog treat") || n.contains("dog toy") || n.contains("dog bandana")) return UITheme.LOCAL_PET;
        if (n.contains("tray") || n.contains("traveler") || n.contains("platter") || n.contains("box") || n.contains("jug")) return UITheme.LOCAL_CATERING;
        if (n.contains("local") || n.contains("national") || n.contains("blend") || n.contains("latte") || n.contains("mocha")
            || n.contains("frappe") || n.contains("chai") || n.contains("bagel") || n.contains("wrap") || n.contains("sandwich")
            || n.contains("donut") || n.contains("shot")) return UITheme.LOCAL_ITEM;
        return UITheme.LOCAL_DEFAULT;
    }

    /** Static "News & Promos" board for the Featured tab — informational only, not purchasable. */
    private void showFeaturedNews() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel(I18n.t("This Month's Happenings"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(UITheme.SECTION_HEADER_COLOR);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(12));

        String[][] rows = {
            {"Week 1", "Order ahead 3x this week for bonus rewards points"},
            {"Week 2", "Free reusable cup with any Fan Favorites purchase"},
            {"Week 2", "Double points on all bakery items, weekends only"},
            {"Week 3", "New seasonal Limited Time Offers arrive in store"},
            {"Week 3", "Happy Hour: extra points on beverages after 1pm"},
            {"Week 4", "Bundle Days: stock up with Value Bundles, extra savings"},
        };

        JPanel table = new JPanel(new GridLayout(0, 2, 10, 8));
        table.setOpaque(false);
        table.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String[] row : rows) {
            JLabel week = new JLabel(row[0]);
            week.setFont(week.getFont().deriveFont(Font.BOLD, 14f));
            week.setForeground(UITheme.DEPT_TAB_ACTIVE);
            JLabel desc = new JLabel(row[1]);
            desc.setFont(desc.getFont().deriveFont(14f));
            table.add(week);
            table.add(desc);
        }
        content.add(table);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        gridContainer.removeAll();
        gridContainer.add(scroll, BorderLayout.CENTER);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    // ---------- CUSTOMIZE PANEL (CENTER, "CUSTOMIZE") ----------
    private final JLabel customizeTitle = new JLabel("", SwingConstants.CENTER);
    private final JPanel customizeBody = new JPanel();
    private final java.util.Map<Modifier, JButton> modifierButtons = new java.util.LinkedHashMap<>();
    private final java.util.List<JButton> sizeButtons = new ArrayList<>();

    private JComponent buildCustomizePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.MODIFIER_BG);

        customizeTitle.setForeground(Color.WHITE);
        customizeTitle.setFont(customizeTitle.getFont().deriveFont(Font.BOLD, 20f));
        customizeTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
        panel.add(customizeTitle, BorderLayout.NORTH);

        customizeBody.setOpaque(false);
        customizeBody.setLayout(new BoxLayout(customizeBody, BoxLayout.Y_AXIS));
        panel.add(new JScrollPane(customizeBody) {{ setOpaque(false); getViewport().setOpaque(false); setBorder(null); }},
            BorderLayout.CENTER);

        JButton doneBtn = UITheme.styledButton(I18n.t("Item Done"), UITheme.DONE_GREEN);
        doneBtn.setFont(doneBtn.getFont().deriveFont(18f));
        doneBtn.setPreferredSize(new Dimension(0, 60));
        doneBtn.addActionListener(e -> confirmCustomize());
        panel.add(doneBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void openCustomize(MenuItem item) {
        pendingItem = item;
        pendingSize = allSizes.isEmpty() ? null : allSizes.get(0);
        pendingTemp = "Hot";
        pendingModifiers.clear();
        customizeTitle.setText(item.getName());
        rebuildCustomizeBody();
        centerCards.show(centerPanel, "CUSTOMIZE");
    }

    /** Rebuilds the size / temp / modifier button rows from the DB-loaded options. */
    private void rebuildCustomizeBody() {
        customizeBody.removeAll();
        sizeButtons.clear();
        modifierButtons.clear();

        if (!allSizes.isEmpty()) {
            customizeBody.add(sectionLabel(I18n.t("Size")));
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
            row.setOpaque(false);
            for (Size size : allSizes) {
                String label = size.getName() + (size.getPriceDelta().signum() > 0 ? " (+$" + size.getPriceDelta() + ")" : "");
                JButton b = UITheme.styledButton(label, UITheme.SIZE_BG);
                b.setPreferredSize(new Dimension(150, 56));
                b.addActionListener(e -> { pendingSize = size; highlightSelectedSize(); });
                sizeButtons.add(b);
                row.add(b);
            }
            customizeBody.add(row);
            highlightSelectedSize();
        }

        customizeBody.add(sectionLabel(I18n.t("Temperature")));
        JPanel tempRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        tempRow.setOpaque(false);
        for (String temp : new String[]{"Hot", "Iced"}) {
            JButton b = UITheme.styledButton(I18n.t(temp), UITheme.ACCENT_RED);
            b.setPreferredSize(new Dimension(120, 56));
            b.addActionListener(e -> pendingTemp = temp);
            tempRow.add(b);
        }
        customizeBody.add(tempRow);

        // Group modifiers (Dairy/Sweetener, Flavor Swirl, Add-On, ...) into their own rows
        java.util.LinkedHashMap<String, java.util.List<Modifier>> byGroup = new java.util.LinkedHashMap<>();
        for (Modifier m : allModifiers) byGroup.computeIfAbsent(m.getGroup(), g -> new ArrayList<>()).add(m);

        for (var entry : byGroup.entrySet()) {
            customizeBody.add(sectionLabel(entry.getKey()));
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
            row.setOpaque(false);
            for (Modifier m : entry.getValue()) {
                JButton b = UITheme.styledButton(m.toString(), UITheme.KEYPAD_BG);
                b.setPreferredSize(new Dimension(190, 50));
                b.addActionListener(e -> {
                    if (pendingModifiers.contains(m)) pendingModifiers.remove(m);
                    else pendingModifiers.add(m);
                    refreshModifierHighlight();
                });
                modifierButtons.put(m, b);
                row.add(b);
            }
            customizeBody.add(row);
        }

        customizeBody.revalidate();
        customizeBody.repaint();
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setBorder(BorderFactory.createEmptyBorder(8, 16, 0, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void highlightSelectedSize() {
        for (int i = 0; i < allSizes.size(); i++) {
            boolean selected = allSizes.get(i).equals(pendingSize);
            sizeButtons.get(i).setBackground(selected ? UITheme.DONE_GREEN : UITheme.SIZE_BG);
        }
    }

    private void refreshModifierHighlight() {
        for (var e : modifierButtons.entrySet()) {
            e.getValue().setBackground(pendingModifiers.contains(e.getKey()) ? UITheme.DONE_GREEN : UITheme.KEYPAD_BG);
        }
    }

    private void confirmCustomize() {
        if (pendingItem == null) return;
        CartItem ci = new CartItem(pendingItem, 1);
        ci.setSize(pendingSize);
        ci.setTemperature(pendingTemp);
        for (Modifier m : pendingModifiers) ci.addModifier(m);
        cart.add(ci);
        refreshCartList();
        pendingItem = null;
        centerCards.show(centerPanel, "GRID");
    }

    // ---------- CART DISPLAY / TOTALS ----------
    private void refreshCartList() {
        cartListPanel.removeAll();
        cartListPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (CartItem ci : cart) {
            JLabel line1 = new JLabel("1  " + ci.getDisplayName());
            line1.setFont(line1.getFont().deriveFont(Font.BOLD, 13f));
            line1.setAlignmentX(Component.LEFT_ALIGNMENT);
            cartListPanel.add(line1);

            String summary = ci.getModifierSummary();
            if (!summary.isEmpty()) {
                JLabel line2 = new JLabel("     " + summary);
                line2.setFont(line2.getFont().deriveFont(Font.PLAIN, 12f));
                line2.setForeground(Color.DARK_GRAY);
                line2.setAlignmentX(Component.LEFT_ALIGNMENT);
                cartListPanel.add(line2);
            }

            JLabel price = new JLabel("     $" + ci.getUnitPrice());
            price.setFont(price.getFont().deriveFont(Font.PLAIN, 12f));
            price.setAlignmentX(Component.LEFT_ALIGNMENT);
            cartListPanel.add(price);
            cartListPanel.add(Box.createVerticalStrut(8));
        }
        cartListPanel.revalidate();
        cartListPanel.repaint();
        recalcTotals();
    }

    private void recalcTotals() {
        BigDecimal subtotal = posService.calculateSubtotal(cart);
        BigDecimal tax = posService.calculateTax(afterDiscountAmount(subtotal));
        BigDecimal total = currentOrderTotal();

        subtotalLabel.setText(I18n.t("Subtotal") + "   $" + subtotal);
        taxLabel.setText(I18n.t("Tax") + "            $" + tax);
        totalLabel.setText(I18n.t("Total") + "         $" + total);
    }

    private BigDecimal afterDiscountAmount(BigDecimal subtotal) {
        BigDecimal afterDiscount = subtotal.subtract(parseDiscount());
        return afterDiscount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : afterDiscount;
    }

    /** The live order total (subtotal - discount + tax) as currently shown on screen - used both
     *  to render the Total label and to validate cash tendered before checkout. */
    private BigDecimal currentOrderTotal() {
        BigDecimal subtotal = posService.calculateSubtotal(cart);
        BigDecimal afterDiscount = afterDiscountAmount(subtotal);
        BigDecimal tax = posService.calculateTax(afterDiscount);
        return afterDiscount.add(tax);
    }

    private BigDecimal parseDiscount() {
        try {
            return new BigDecimal(discountField.getText().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void openLoyaltyDialog() {
        JTextField nameField = new JTextField(pendingCustomer != null ? pendingCustomer.getName() : "");
        JTextField phoneField = new JTextField(pendingCustomer != null ? pendingCustomer.getPhone() : "");
        JTextField emailField = new JTextField(pendingCustomer != null ? pendingCustomer.getEmail() : "");

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Phone:")); form.add(phoneField);
        form.add(new JLabel("Email (optional):")); form.add(emailField);

        int result = JOptionPane.showConfirmDialog(this, form,
            "Register / Apply Loyalty Points", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and phone are required.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!phone.matches("[0-9+()\\-\\s]{7,20}")) {
            JOptionPane.showMessageDialog(this, "Enter a valid phone number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            JOptionPane.showMessageDialog(this, "Enter a valid email or leave it blank.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            pendingCustomer = customerDAO.findOrCreate(name, phone, email.isEmpty() ? null : email);
            loyaltyBtn.setText(pendingCustomer.getName() + " (" + pendingCustomer.getLoyaltyPoints() + " pts)");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not save customer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Prompts for a delivery address when Delivery is chosen; reverts to Dine In if the user backs out. */
    private void onOrderTypeChanged() {
        if ("DELIVERY".equals(orderTypeBox.getSelectedItem())) {
            String address = JOptionPane.showInputDialog(this, "Delivery address:", deliveryAddress);
            if (address == null || address.trim().isEmpty()) {
                orderTypeBox.setSelectedItem("DINE_IN");
                deliveryAddress = null;
            } else {
                deliveryAddress = address.trim();
            }
        } else {
            deliveryAddress = null;
        }
    }

    /** Opens the colorful "Payments" screen (keypad + Credit Card / CASH / GC Redeem, plus a
     *  Payments-related sidebar) in place of the item grid — mirrors the real POS's payment
     *  screen, reached by pressing PAY. Validates the cart/order first, same as the old
     *  single-dialog checkout used to. */
    private void openPaymentsScreen() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.", "Nothing to checkout", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String orderType = (String) orderTypeBox.getSelectedItem();
        if ("DELIVERY".equals(orderType) && (deliveryAddress == null || deliveryAddress.isEmpty())) {
            JOptionPane.showMessageDialog(this, "A delivery address is required for delivery orders.",
                "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        paymentAmountField.setText("");
        taxExempt = false;
        activePaymentsGroup = "Payments";
        highlightDeptTab("__PAYMENTS__");
        rebuildPaymentsSidebar("Payments");
        showPaymentsGroup("Payments");
        centerCards.show(centerPanel, "GRID");
    }

    /** Leaves the Payments screen and restores whatever department/subcategory grid was showing
     *  before PAY was pressed. */
    private void exitPaymentsScreen() {
        activePaymentsGroup = null;
        if (currentDepartment == null) {
            centerCards.show(centerPanel, "GRID");
            return;
        }
        highlightDeptTab(currentDepartment.getName());
        List<Category> children = menuItemDAO.getChildCategories(currentDepartment.getId());
        if (!children.isEmpty()) {
            rebuildSidebar(children);
            if (currentSubcategory != null) {
                highlightSidebarItem(currentSubcategory.getName());
                showItemsForCategory(currentSubcategory);
            } else {
                selectSubcategory(children.get(0));
            }
        } else {
            rebuildSidebar(null);
            showItemsForCategory(currentDepartment);
        }
        centerCards.show(centerPanel, "GRID");
    }

    private void rebuildPaymentsSidebar(String activeName) {
        paymentsSidebarButtons.clear();
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        for (String name : PAYMENTS_SIDEBAR_ORDER) {
            JButton b = UITheme.styledButton(I18n.t(name), UITheme.SIDEBAR_ITEM_BG);
            b.setForeground(UITheme.DEPT_TAB_TEXT);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(150, 56));
            b.addActionListener(e -> {
                activePaymentsGroup = name;
                rebuildPaymentsSidebar(name);
                showPaymentsGroup(name);
            });
            paymentsSidebarButtons.put(name, b);
            inner.add(b);
            inner.add(Box.createVerticalStrut(6));
        }
        inner.add(Box.createVerticalGlue());
        JPanel bg = UITheme.gradientPanel(UITheme.SIDEBAR_BG, new Color(250, 226, 196));
        bg.add(inner, BorderLayout.CENTER);
        sidebarContainer.removeAll();
        sidebarContainer.add(bg, BorderLayout.CENTER);
        sidebarContainer.revalidate();
        sidebarContainer.repaint();
        for (var entry : paymentsSidebarButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeName);
            entry.getValue().setBackground(active ? UITheme.SIDEBAR_ACTIVE : UITheme.SIDEBAR_ITEM_BG);
            entry.getValue().setForeground(active ? Color.WHITE : UITheme.DEPT_TAB_TEXT);
        }
    }

    private void showPaymentsGroup(String groupName) {
        JPanel content;
        switch (groupName) {
            case "Payments":
                content = buildPaymentsKeypadContent();
                break;
            case "Other Payments":
                content = paymentsPlaceholderContent("Other Payments", UITheme.PAY_OTHER,
                        "Check Payment", "House Account", "Employee Meal", "Comp");
                break;
            case "Coupons/Discounts":
                content = paymentsPlaceholderContent("Coupons/Discounts", UITheme.PAY_COUPON,
                        "Apply Coupon", "Percent Off Discount", "Dollar Off Discount", "Employee Discount");
                break;
            case "Gift Card Functions":
                content = paymentsPlaceholderContent("Gift Card Functions", UITheme.PAY_GIFTFUNC,
                        "Sell Gift Card", "Reload Gift Card", "Check Gift Card Balance", "Void Gift Card Sale");
                break;
            case "Service Charges":
                content = paymentsPlaceholderContent("Service Charges", UITheme.PAY_SERVICE,
                        "Add Service Charge", "Add Delivery Fee", "Add Gratuity");
                break;
            case "Delivery Payments":
                content = paymentsPlaceholderContent("Delivery Payments", UITheme.PAY_DELIVERY,
                        "Cash on Delivery", "Prepaid Delivery", "Delivery Refund");
                break;
            default:
                content = new JPanel();
                content.setOpaque(false);
        }
        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        gridContainer.removeAll();
        gridContainer.add(scroll, BorderLayout.CENTER);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private JPanel paymentsPlaceholderContent(String header, Color color, String... labels) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        String bannerLabel = I18n.t(header);
        JButton banner = UITheme.styledButton(bannerLabel, color);
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        banner.setPreferredSize(new Dimension(600, 54));
        banner.setFont(banner.getFont().deriveFont(Font.BOLD, 15f));
        banner.addActionListener(e -> JOptionPane.showMessageDialog(this,
                bannerLabel + " " + I18n.t("completed."), bannerLabel, JOptionPane.INFORMATION_MESSAGE));
        content.add(Box.createVerticalStrut(8));
        content.add(banner);
        content.add(Box.createVerticalStrut(4));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String rawLabel : labels) {
            String label = I18n.t(rawLabel);
            JButton btn = UITheme.styledButton("<html><center>" + label + "</center></html>", color);
            btn.setPreferredSize(new Dimension(150, 70));
            btn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    label + " " + I18n.t("completed."), label, JOptionPane.INFORMATION_MESSAGE));
            row.add(btn);
        }
        content.add(row);
        return content;
    }

    /** The main "Payments" screen: amount keypad on the left, Credit Card / CASH / GC Redeem
     *  on the right, and Tax Exempt / Cancel Saved-Stored Order / MAIN MENU along the bottom —
     *  mirrors the real POS's payment screen layout. */
    private JPanel buildPaymentsKeypadContent() {
        JPanel content = new JPanel(new BorderLayout(20, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel banner = new JLabel(I18n.t("Payments"), SwingConstants.CENTER);
        banner.setOpaque(true);
        banner.setBackground(UITheme.PAY_BANNER);
        banner.setForeground(Color.WHITE);
        banner.setFont(banner.getFont().deriveFont(Font.BOLD, 20f));
        banner.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
        content.add(banner, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(30, 0));
        middle.setOpaque(false);

        // ---- Keypad column ----
        JPanel keypadCol = new JPanel();
        keypadCol.setLayout(new BoxLayout(keypadCol, BoxLayout.Y_AXIS));
        keypadCol.setOpaque(false);

        JPanel displayRow = new JPanel(new BorderLayout(8, 0));
        displayRow.setOpaque(false);
        paymentAmountField.setFont(paymentAmountField.getFont().deriveFont(20f));
        paymentAmountField.setEditable(false);
        JButton backBtn = UITheme.styledButton(I18n.t("BACK"), UITheme.PAY_KEYPAD);
        backBtn.setPreferredSize(new Dimension(90, 50));
        backBtn.addActionListener(e -> {
            String t = paymentAmountField.getText();
            if (!t.isEmpty()) paymentAmountField.setText(t.substring(0, t.length() - 1));
        });
        displayRow.add(paymentAmountField, BorderLayout.CENTER);
        displayRow.add(backBtn, BorderLayout.EAST);
        displayRow.setMaximumSize(new Dimension(300, 50));
        displayRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        keypadCol.add(displayRow);
        keypadCol.add(Box.createVerticalStrut(10));

        JPanel numGrid = new JPanel(new GridLayout(3, 3, 8, 8));
        numGrid.setOpaque(false);
        numGrid.setMaximumSize(new Dimension(300, 180));
        numGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String d : new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"}) {
            JButton b = UITheme.styledButton(d, UITheme.PAY_KEYPAD);
            b.addActionListener(e -> paymentAmountField.setText(paymentAmountField.getText() + d));
            numGrid.add(b);
        }
        keypadCol.add(numGrid);
        keypadCol.add(Box.createVerticalStrut(8));

        JPanel bottomNumRow = new JPanel(new GridLayout(1, 3, 8, 8));
        bottomNumRow.setOpaque(false);
        bottomNumRow.setMaximumSize(new Dimension(300, 58));
        bottomNumRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String d : new String[]{".", "0", "00"}) {
            JButton b = UITheme.styledButton(d, UITheme.PAY_KEYPAD);
            b.addActionListener(e -> paymentAmountField.setText(paymentAmountField.getText() + d));
            bottomNumRow.add(b);
        }
        keypadCol.add(bottomNumRow);
        keypadCol.add(Box.createVerticalStrut(8));

        JButton clearBtn = UITheme.styledButton(I18n.t("Clear/No"), UITheme.PAY_KEYPAD);
        clearBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearBtn.setMaximumSize(new Dimension(300, 58));
        clearBtn.addActionListener(e -> paymentAmountField.setText(""));
        keypadCol.add(clearBtn);

        middle.add(keypadCol, BorderLayout.WEST);

        // ---- Payment method column ----
        JPanel actionCol = new JPanel();
        actionCol.setLayout(new BoxLayout(actionCol, BoxLayout.Y_AXIS));
        actionCol.setOpaque(false);

        JButton creditBtn = UITheme.styledButton(I18n.t("Credit Card"), UITheme.PAY_CREDIT);
        creditBtn.setFont(creditBtn.getFont().deriveFont(Font.BOLD, 16f));
        creditBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        creditBtn.setMaximumSize(new Dimension(220, 60));
        creditBtn.addActionListener(e -> performCheckout("CARD"));
        actionCol.add(creditBtn);
        actionCol.add(Box.createVerticalStrut(14));

        JButton cashBtn = UITheme.styledButton(I18n.t("CASH"), UITheme.PAY_CASH);
        cashBtn.setFont(cashBtn.getFont().deriveFont(Font.BOLD, 16f));
        cashBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cashBtn.setMaximumSize(new Dimension(220, 60));
        cashBtn.addActionListener(e -> performCheckout("CASH"));
        actionCol.add(cashBtn);
        actionCol.add(Box.createVerticalStrut(14));

        JButton gcBtn = UITheme.styledButton(I18n.t("GC Redeem"), UITheme.PAY_GC);
        gcBtn.setFont(gcBtn.getFont().deriveFont(Font.BOLD, 16f));
        gcBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        gcBtn.setMaximumSize(new Dimension(220, 60));
        gcBtn.addActionListener(e -> {
            String code = JOptionPane.showInputDialog(this, I18n.t("Enter gift card number:"), I18n.t("GC Redeem"),
                    JOptionPane.QUESTION_MESSAGE);
            if (code != null && !code.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Gift card ending " +
                        code.substring(Math.max(0, code.length() - 4)) + " applied.",
                        "GC Redeem", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        actionCol.add(gcBtn);

        middle.add(actionCol, BorderLayout.CENTER);
        content.add(middle, BorderLayout.CENTER);

        // ---- Bottom row: Tax Exempt / Cancel Saved-Stored Order (left), MAIN MENU (right) ----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        bottomLeft.setOpaque(false);
        JButton taxExemptBtn = UITheme.styledButton(I18n.t("Tax Exempt"), UITheme.PAY_TAXEXEMPT);
        taxExemptBtn.setPreferredSize(new Dimension(140, 60));
        taxExemptBtn.addActionListener(e -> {
            taxExempt = !taxExempt;
            taxExemptBtn.setText(taxExempt ? I18n.t("Tax Exempt") + " (ON)" : I18n.t("Tax Exempt"));
        });
        JButton cancelSavedBtn = UITheme.styledButton(
                "<html><center>" + I18n.t("Cancel Saved/Stored Order") + "</center></html>", UITheme.PAY_CANCELSAVED);
        cancelSavedBtn.setPreferredSize(new Dimension(140, 60));
        cancelSavedBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, I18n.t("Cancel this order?"),
                    I18n.t("Cancel Saved/Stored Order"), JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                cart.clear();
                discountField.setText("0.00");
                refreshCartList();
                exitPaymentsScreen();
            }
        });
        bottomLeft.add(taxExemptBtn);
        bottomLeft.add(cancelSavedBtn);
        bottom.add(bottomLeft, BorderLayout.WEST);

        JButton mainMenuBtn = UITheme.styledButton(I18n.t("MAIN MENU"), UITheme.HEADER_BG);
        mainMenuBtn.setPreferredSize(new Dimension(140, 44));
        mainMenuBtn.addActionListener(e -> exitPaymentsScreen());
        JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        bottomRight.setOpaque(false);
        bottomRight.add(mainMenuBtn);
        bottom.add(bottomRight, BorderLayout.EAST);

        content.add(bottom, BorderLayout.SOUTH);
        return content;
    }

    /** Runs the real checkout (same DB/order logic the old single-dialog checkout used), then
     *  returns to the item grid on success. Payment `method` must match the DB's
     *  payments.method ENUM('CASH','CARD','MOBILE_BANKING'). */
    private void performCheckout(String method) {
        String orderType = (String) orderTypeBox.getSelectedItem();
        String orderSource = (String) orderSourceBox.getSelectedItem();

        // Cash sales: require the cashier to key in what the customer handed over on the
        // keypad first, and refuse to proceed if it doesn't cover the total - exactly like a
        // real register - so we can compute and show the change due back to the customer.
        // If the cash handed over falls short, offer to split the sale: keep the cash as a
        // partial payment and charge the remaining balance to a credit card instead.
        BigDecimal cashTendered = null;
        BigDecimal splitCardAmount = null;
        if ("CASH".equals(method)) {
            String typed = paymentAmountField.getText().trim();
            if (typed.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Enter the cash amount the customer handed over on the keypad first.",
                    "Cash amount required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                cashTendered = new BigDecimal(typed);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "\"" + typed + "\" isn't a valid amount.",
                    "Invalid amount", JOptionPane.WARNING_MESSAGE);
                return;
            }
            BigDecimal dueNow = currentOrderTotal();
            if (cashTendered.compareTo(dueNow) < 0) {
                BigDecimal remainder = dueNow.subtract(cashTendered).setScale(2, java.math.RoundingMode.HALF_UP);
                int choice = JOptionPane.showConfirmDialog(this,
                    "Cash tendered ($" + cashTendered.setScale(2, java.math.RoundingMode.HALF_UP) +
                    ") is less than the amount due ($" + dueNow + ").\n\n" +
                    "Charge the remaining $" + remainder + " to a credit card to complete this sale as a split payment?",
                    "Split payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
                splitCardAmount = remainder;
            }
        }

        try {
            POSService.CheckoutResult result;
            if (splitCardAmount != null) {
                result = pendingCustomer != null
                    ? posService.checkoutSplitCashCardWithLoyalty(cart, parseDiscount(), currentUser.getId(), pendingCustomer, cashTendered, orderSource, orderType)
                    : posService.checkoutSplitCashCard(cart, parseDiscount(), currentUser.getId(), null, cashTendered, orderSource, orderType);
            } else {
                result = pendingCustomer != null
                    ? posService.checkoutWithLoyalty(cart, parseDiscount(), currentUser.getId(), pendingCustomer, method, orderSource, orderType)
                    : posService.checkout(cart, parseDiscount(), currentUser.getId(), null, method, orderSource, orderType);
            }

            if ("DELIVERY".equals(orderType)) {
                deliveryDAO.createDelivery(result.orderId, deliveryAddress);
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Order #").append(result.orderId).append(" completed.\n");
            msg.append("Total: $").append(result.total).append("\n");
            msg.append("Payment ref: ").append(result.paymentReference);
            if (result.cashPortion != null) {
                msg.append("\nCash Applied: $").append(result.cashPortion);
                msg.append("\nCard Charged: $").append(result.cardPortion);
                msg.append("\nChange Due: $0.00");
            } else if (cashTendered != null) {
                BigDecimal change = cashTendered.subtract(result.total).setScale(2, java.math.RoundingMode.HALF_UP);
                msg.append("\nCash Tendered: $").append(cashTendered.setScale(2, java.math.RoundingMode.HALF_UP));
                msg.append("\nChange Due: $").append(change);
            }
            if ("DELIVERY".equals(orderType)) {
                msg.append("\n\nSent to Delivery Queue (").append(deliveryAddress).append(").");
            }
            if (result.loyaltyRewardApplied) {
                msg.append("\n\nLoyalty reward! Cheapest item was made FREE and points reset to 0.");
            } else if (result.loyaltyPointsAfter >= 0) {
                msg.append("\n\nLoyalty points: ").append(result.loyaltyPointsAfter).append(" / ")
                   .append(POSService.LOYALTY_POINTS_FOR_REWARD);
            }

            JOptionPane.showMessageDialog(this, msg.toString(), "Checkout successful", JOptionPane.INFORMATION_MESSAGE);

            cart.clear();
            discountField.setText("0.00");
            pendingCustomer = null;
            loyaltyBtn.setText("+ Loyalty Customer");
            orderTypeBox.setSelectedItem("DINE_IN");
            orderSourceBox.setSelectedItem("IN_STORE");
            deliveryAddress = null;
            paymentAmountField.setText("");
            refreshCartList();
            exitPaymentsScreen();

        } catch (OrderDAO.InsufficientStockException ex) {
            JOptionPane.showMessageDialog(this, "Checkout failed.\nDetails: " + ex.getMessage(),
                "Insufficient stock", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Checkout failed.\nDetails: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
