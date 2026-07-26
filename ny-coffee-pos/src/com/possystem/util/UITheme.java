package com.possystem.util;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UITheme {
    public static final Color CART_BG = new Color(230, 233, 236);
    public static final Color HEADER_BG = new Color(30, 42, 58);
    public static final Color ACCENT_RED = new Color(196, 30, 42);
    public static final Color CATEGORY_BG = new Color(52, 64, 82);
    public static final Color CATEGORY_ACTIVE = new Color(196, 30, 42);
    public static final Color MODIFIER_BG = new Color(45, 58, 74);
    public static final Color KEYPAD_BG = new Color(41, 128, 185);
    public static final Color SIZE_BG = new Color(52, 73, 94);
    public static final Color PAY_GREEN = new Color(39, 142, 79);
    public static final Color CASH_GREEN = new Color(46, 160, 90);
    public static final Color DONE_GREEN = new Color(46, 160, 90);
    public static final Color TEXT_LIGHT = Color.WHITE;

    // ---- Department tabs / subcategory sidebar / item grid (POS checkout screen) ----
    public static final Color DEPT_TAB_BAR_BG = new Color(255, 250, 240);
    public static final Color DEPT_TAB_BG = new Color(250, 235, 215);
    public static final Color DEPT_TAB_ACTIVE = new Color(214, 80, 40);
    public static final Color DEPT_TAB_TEXT = new Color(60, 40, 30);
    public static final Color SIDEBAR_BG = new Color(255, 244, 230);
    public static final Color SIDEBAR_ITEM_BG = new Color(255, 255, 255);
    public static final Color SIDEBAR_ACTIVE = new Color(214, 80, 40);
    public static final Color GRID_GRADIENT_TOP = new Color(255, 247, 235);
    public static final Color GRID_GRADIENT_BOTTOM = new Color(255, 216, 168);
    public static final Color SECTION_HEADER_COLOR = new Color(140, 78, 30);
    public static final Color ITEM_CARD_BG = new Color(255, 255, 255);
    public static final Color ITEM_CARD_BORDER = new Color(214, 80, 40);

    // ---- Beverages tab: colorful per-item buttons, grouped by section within each
    // subcategory (Coffee/Espresso/Teas/Refreshers/Frozen/Other Beverages) ----
    public static final Color BEV_COFFEE_BLEND = new Color(109, 76, 65);
    public static final Color BEV_COFFEE_COLDBREW = new Color(62, 39, 35);
    public static final Color BEV_COFFEE_ICED = new Color(3, 155, 229);
    public static final Color BEV_COFFEE_HOTFLAVORED = new Color(230, 126, 34);
    public static final Color BEV_COFFEE_DECAF = new Color(161, 136, 127);
    public static final Color BEV_ESP_SINGLE = new Color(93, 64, 55);
    public static final Color BEV_ESP_DOUBLE = new Color(102, 33, 33);
    public static final Color BEV_ESP_ALT = new Color(0, 121, 107);
    public static final Color BEV_ESP_SEASONAL = new Color(191, 54, 12);
    public static final Color BEV_TEA_ICED = new Color(0, 150, 136);
    public static final Color BEV_TEA_HOT = new Color(141, 110, 99);
    public static final Color BEV_TEA_CHAI_MATCHA = new Color(85, 139, 47);
    public static final Color BEV_TEA_BUBBLE = new Color(156, 39, 176);
    public static final Color BEV_REF_CLASSIC = new Color(211, 47, 47);
    public static final Color BEV_REF_DAYDREAM = new Color(240, 98, 146);
    public static final Color BEV_REF_ENERGY = new Color(124, 179, 66);
    public static final Color BEV_REF_SPLASH = new Color(0, 172, 193);
    public static final Color BEV_REF_MIXIT = new Color(255, 143, 0);
    public static final Color BEV_REF_SPARKLING = new Color(38, 166, 154);
    public static final Color BEV_FROZEN_COOLERS = new Color(41, 121, 255);
    public static final Color BEV_FROZEN_COFFEE = new Color(78, 52, 46);
    public static final Color BEV_FROZEN_SPECIALTY = new Color(106, 27, 154);
    public static final Color BEV_FROZEN_OTHER = new Color(0, 151, 167);
    public static final Color BEV_FROZEN_FRUIT = new Color(216, 27, 96);
    public static final Color BEV_OTHER_FOUNTAIN = new Color(21, 101, 192);
    public static final Color BEV_OTHER_ZERO = new Color(139, 195, 74);
    public static final Color BEV_OTHER_BULK = new Color(121, 85, 72);
    public static final Color BEV_OTHER_BOTTLED = new Color(251, 140, 0);
    public static final Color BEV_OTHER_MILK = new Color(191, 143, 82);
    public static final Color BEV_OTHER_KIDS = new Color(233, 30, 99);
    public static final Color BEV_DEFAULT = new Color(230, 126, 34);

    // ---- Featured tab: colorful per-item buttons, grouped by drink type ----
    public static final Color FEATURED_REFRESHER = new Color(211, 47, 47);
    public static final Color FEATURED_LATTE = new Color(121, 85, 72);
    public static final Color FEATURED_MATCHA = new Color(67, 160, 71);
    public static final Color FEATURED_LEMONADE = new Color(249, 168, 37);
    public static final Color FEATURED_ZERO = new Color(38, 50, 71);
    public static final Color FEATURED_CHILLER = new Color(123, 31, 162);
    public static final Color FEATURED_BUNDLE = new Color(0, 121, 107);
    public static final Color FEATURED_DEFAULT = new Color(230, 126, 34);

    // ---- Bakery tab: colorful per-item buttons, grouped by treat type ----
    public static final Color BAKERY_DONUT = new Color(216, 67, 21);
    public static final Color BAKERY_DONUT_HOLE = new Color(106, 27, 154);
    public static final Color BAKERY_FANCY = new Color(0, 137, 123);
    public static final Color BAKERY_BAGEL = new Color(25, 118, 210);
    public static final Color BAKERY_MUFFIN = new Color(85, 139, 47);
    public static final Color BAKERY_SPREAD = new Color(141, 110, 99);
    public static final Color BAKERY_COOKIE = new Color(93, 64, 55);
    public static final Color BAKERY_PASTRY = new Color(194, 24, 91);
    public static final Color BAKERY_TREAT = new Color(249, 168, 37);
    public static final Color BAKERY_SNACK = new Color(117, 121, 34);

    // ---- Sandwiches tab: colorful per-item buttons, grouped by item type ----
    public static final Color SANDWICH_BREAKFAST = new Color(230, 81, 0);
    public static final Color SANDWICH_MELT = new Color(191, 54, 12);
    public static final Color SANDWICH_DELI = new Color(93, 64, 55);
    public static final Color SANDWICH_WRAP = new Color(56, 142, 60);
    public static final Color SANDWICH_BOWL = new Color(0, 121, 107);
    public static final Color SANDWICH_HASHBROWN = new Color(255, 143, 0);
    public static final Color SANDWICH_BITE = new Color(156, 39, 176);
    public static final Color SANDWICH_SIDE = new Color(121, 85, 72);
    public static final Color SANDWICH_SNACK = new Color(211, 47, 47);
    public static final Color SANDWICH_DEFAULT = new Color(97, 97, 97);

    // ---- Retail tab: colorful per-item buttons, grouped by product type ----
    public static final Color RETAIL_COFFEE = new Color(109, 76, 65);
    public static final Color RETAIL_TEA = new Color(0, 137, 123);
    public static final Color RETAIL_KCUP = new Color(255, 143, 0);
    public static final Color RETAIL_BREWER = new Color(69, 90, 100);
    public static final Color RETAIL_MUG = new Color(216, 67, 21);
    public static final Color RETAIL_DRINKWARE = new Color(2, 119, 189);
    public static final Color RETAIL_GIFT = new Color(194, 24, 91);
    public static final Color RETAIL_HOLIDAY = new Color(198, 40, 40);
    public static final Color RETAIL_SNACK = new Color(85, 139, 47);
    public static final Color RETAIL_CANDY = new Color(123, 31, 162);
    public static final Color RETAIL_BOTTLED = new Color(2, 136, 209);
    public static final Color RETAIL_CARD = new Color(0, 121, 107);
    public static final Color RETAIL_MERCH = new Color(255, 179, 0);
    public static final Color RETAIL_ACCESSORY = new Color(120, 144, 156);
    public static final Color RETAIL_DEFAULT = new Color(97, 97, 97);

    // ---- Local tab: colorful per-item buttons, grouped by item type ----
    public static final Color LOCAL_DONATION = new Color(211, 47, 47);
    public static final Color LOCAL_BAGFEE = new Color(56, 142, 60);
    public static final Color LOCAL_PET = new Color(123, 31, 162);
    public static final Color LOCAL_CATERING = new Color(230, 81, 0);
    public static final Color LOCAL_ITEM = new Color(0, 121, 107);
    public static final Color LOCAL_DEFAULT = new Color(97, 97, 97);

    // ---- Functions tab: colorful per-button groups, mirroring a manager function board ----
    public static final Color FUNC_APP = new Color(69, 90, 100);
    public static final Color FUNC_BARCODE = new Color(0, 121, 107);
    public static final Color FUNC_DRAWER = new Color(230, 126, 34);
    public static final Color FUNC_ORDERTYPE = new Color(211, 47, 47);
    public static final Color FUNC_EMPLOYEE = new Color(41, 128, 185);
    public static final Color FUNC_TIMECLOCK = new Color(3, 169, 244);
    public static final Color FUNC_CHECK = new Color(123, 31, 162);
    public static final Color FUNC_SMARTSELL = new Color(67, 160, 71);
    public static final Color FUNC_NOTIFY = new Color(255, 143, 0);
    public static final Color FUNC_MENU = new Color(93, 64, 55);
    public static final Color FUNC_PHONE = new Color(249, 168, 37);
    public static final Color FUNC_GIFTCARD = new Color(56, 142, 60);
    public static final Color FUNC_SHIFT = new Color(0, 150, 136);
    public static final Color FUNC_STATUS = new Color(3, 155, 229);
    public static final Color FUNC_TILL = new Color(67, 160, 71);
    public static final Color FUNC_TILL_UNASSIGN = new Color(211, 47, 47);
    public static final Color FUNC_CASHDASH = new Color(94, 53, 177);
    public static final Color FUNC_SAFE = new Color(2, 119, 189);
    public static final Color FUNC_SHIFTREPORT = new Color(141, 74, 60);
    public static final Color FUNC_REPORTS = new Color(63, 81, 181);
    public static final Color FUNC_ALLREPORTS = new Color(25, 118, 210);
    public static final Color FUNC_DEVICE = new Color(79, 195, 247);
    public static final Color FUNC_CONFIRM_BANNER = new Color(255, 87, 34);
    public static final Color FUNC_CONFIRM_GREEN = new Color(67, 160, 71);
    public static final Color FUNC_CONFIRM_RED = new Color(211, 47, 47);

    // ---- Payments screen (opened from the PAY button): keypad + payment method buttons,
    // plus a payment-related sidebar (Payments/Other Payments/Coupons & Discounts/...) ----
    public static final Color PAY_BANNER = new Color(3, 169, 244);
    public static final Color PAY_KEYPAD = new Color(84, 110, 122);
    public static final Color PAY_CREDIT = new Color(56, 142, 60);
    public static final Color PAY_CASH = new Color(67, 160, 71);
    public static final Color PAY_GC = new Color(94, 53, 177);
    public static final Color PAY_TAXEXEMPT = new Color(56, 142, 60);
    public static final Color PAY_CANCELSAVED = new Color(211, 47, 47);
    public static final Color PAY_OTHER = new Color(0, 137, 123);
    public static final Color PAY_COUPON = new Color(230, 81, 0);
    public static final Color PAY_GIFTFUNC = new Color(106, 27, 154);
    public static final Color PAY_SERVICE = new Color(121, 85, 72);
    public static final Color PAY_DELIVERY = new Color(2, 119, 189);
    public static final Color FUNC_OPENCHECKS = new Color(173, 20, 87);
    public static final Color FUNC_DT = new Color(230, 74, 25);
    public static final Color FUNC_OTG = new Color(106, 27, 154);
    public static final Color FUNC_CONFIRM = new Color(0, 172, 193);
    public static final Color FUNC_SUPPORT = new Color(96, 125, 139);

    // ---- Left sidebar navigation (MainDashboard): one distinct color per menu item ----
    public static final Color NAV_POS = new Color(196, 30, 42);
    public static final Color NAV_MENU_MGMT = new Color(230, 126, 34);
    public static final Color NAV_INVENTORY = new Color(0, 121, 107);
    public static final Color NAV_TIMECLOCK = new Color(3, 169, 244);
    public static final Color NAV_DELIVERY = new Color(2, 119, 189);
    public static final Color NAV_RECIPES = new Color(67, 160, 71);
    public static final Color NAV_REPORTS = new Color(63, 81, 181);
    public static final Color NAV_EMPLOYEES = new Color(41, 128, 185);
    public static final Color NAV_PAYROLL = new Color(156, 39, 176);
    public static final Color NAV_SUPPLIERS = new Color(121, 85, 72);
    public static final Color NAV_ABOUT = new Color(230, 81, 0);
    public static final Color NAV_LOGOUT = new Color(97, 97, 97);

    /** A panel that paints a soft top-to-bottom color gradient behind its (non-opaque) children. */
    public static JPanel gradientPanel(Color top, Color bottom) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    public static void addHoverEffect(AbstractButton button, Color base) {
        Color hover = base.darker();
        button.setBackground(base);
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(base); }
        });
    }

    public static JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        addHoverEffect(b, bg);
        return b;
    }

    /** A soft tint of {@code accent}, blended most of the way toward white — used for
     *  alternating table row stripes so the accent color reads without hurting legibility. */
    public static Color tint(Color accent, double towardWhite) {
        int r = (int) (accent.getRed() + (255 - accent.getRed()) * towardWhite);
        int g = (int) (accent.getGreen() + (255 - accent.getGreen()) * towardWhite);
        int b = (int) (accent.getBlue() + (255 - accent.getBlue()) * towardWhite);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /** Styles a JTable to match one page's accent color: a bold colored header, soft
     *  alternating row stripes tinted from the accent, and a matching selection highlight -
     *  the same colorful-per-section look used throughout the rest of the app, applied to the
     *  data tables on the management screens (Inventory, Menu Management, Payroll, etc.). */
    public static void styleTable(JTable table, Color accent) {
        table.getTableHeader().setBackground(accent);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(24);
        table.setShowGrid(true);
        table.setGridColor(new Color(224, 224, 224));
        table.setSelectionBackground(accent);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        Color stripe = tint(accent, 0.88);
        table.setDefaultRenderer(Object.class, new StripedRenderer(stripe));
    }

    private static class StripedRenderer extends DefaultTableCellRenderer {
        private final Color stripe;
        StripedRenderer(Color stripe) { this.stripe = stripe; }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : stripe);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
