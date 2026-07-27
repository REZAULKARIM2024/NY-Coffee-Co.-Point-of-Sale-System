package com.possystem.gui;

import com.possystem.dao.EmployeeDAO;
import com.possystem.dao.MenuItemDAO;
import com.possystem.dao.SupplierDAO;
import com.possystem.util.CoffeeShopScenePanel;
import com.possystem.util.UITheme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * "About" screen reached from the sidebar: brand illustration on the left (the same
 * procedurally painted coffee-shop scene used to fill the sidebar's empty space), and on the
 * right a short description, app/version info, live counts pulled straight from the database
 * (employees, menu items, active suppliers) so nothing here is stale placeholder copy, and the
 * same support contact info shown on the Functions > SUPPORT screen.
 */
public class AboutPanel extends JPanel {

    private static final String APP_VERSION = "1.0.0";

    public AboutPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.SIDEBAR_BG);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        CoffeeShopScenePanel art = new CoffeeShopScenePanel();
        art.setPreferredSize(new Dimension(220, 0));
        add(art, BorderLayout.WEST);

        add(buildInfoPanel(), BorderLayout.CENTER);
    }

    private JComponent buildInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.SIDEBAR_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        JLabel heading = new JLabel("About NY Coffee Co. POS");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        heading.setForeground(new Color(60, 40, 30));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);

        JLabel version = new JLabel("Version " + APP_VERSION + " — Point of Sale, Payroll & Operations");
        version.setFont(version.getFont().deriveFont(Font.PLAIN, 13f));
        version.setForeground(new Color(120, 90, 60));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(version);

        panel.add(Box.createVerticalStrut(16));

        JTextArea description = new JTextArea(
            "NY Coffee Co. is an independent New York City coffee shop. This application runs " +
            "the register, kitchen and delivery workflow, payroll, recipes, and supplier " +
            "ordering for the shop — everything the team needs to run a shift, front to back."
        );
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setFont(description.getFont().deriveFont(Font.PLAIN, 13f));
        description.setForeground(new Color(70, 55, 45));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setMaximumSize(new Dimension(520, 90));
        panel.add(description);

        panel.add(Box.createVerticalStrut(20));
        panel.add(sectionLabel("Leadership"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(buildLeadershipCard());

        panel.add(Box.createVerticalStrut(20));
        panel.add(sectionLabel("Mission & Vision"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(buildMissionVisionRow());

        panel.add(Box.createVerticalStrut(20));
        panel.add(sectionLabel("At a Glance"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(buildStatsGrid());

        panel.add(Box.createVerticalStrut(20));
        panel.add(sectionLabel("Support"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(supportLine("Phone", "1-800-555-0199 (24/7 for register & payment issues)"));
        panel.add(supportLine("Knowledge Base", "Functions > SUPPORT > Knowledge Base"));

        panel.add(Box.createVerticalStrut(20));
        JLabel tagline = new JLabel("“Brewed in the Heart of NYC.”");
        tagline.setFont(new Font("Serif", Font.ITALIC, 14));
        tagline.setForeground(new Color(140, 78, 30));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(tagline);

        panel.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(new Color(140, 78, 30));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel buildLeadershipCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.X_AXIS));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 210, 190)),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setMaximumSize(new Dimension(520, 150));

        JLabel photo = new JLabel();
        photo.setAlignmentY(Component.CENTER_ALIGNMENT);
        ImageIcon avatar = loadCircularAvatar("/com/possystem/resources/owner.jpg", 84);
        if (avatar != null) {
            photo.setIcon(avatar);
        } else {
            photo.setPreferredSize(new Dimension(84, 84));
            photo.setHorizontalAlignment(SwingConstants.CENTER);
            photo.setText("?");
            photo.setFont(photo.getFont().deriveFont(Font.BOLD, 30f));
            photo.setOpaque(true);
            photo.setBackground(UITheme.tint(UITheme.NAV_ABOUT, 0.8));
        }

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel("REZAUL KARIM");
        name.setFont(name.getFont().deriveFont(Font.BOLD, 17f));
        name.setForeground(new Color(60, 40, 30));

        JLabel title = new JLabel("Owner and Managing Director");
        title.setFont(title.getFont().deriveFont(Font.PLAIN, 12f));
        title.setForeground(UITheme.NAV_ABOUT.darker());

        textCol.add(name);
        textCol.add(Box.createVerticalStrut(4));
        textCol.add(title);
        textCol.add(Box.createVerticalStrut(8));
        textCol.add(contactLine("☎", "+1 631-530-3422", null));
        textCol.add(Box.createVerticalStrut(3));
        textCol.add(contactLine("in", "linkedin.com/in/rezaul-karim-803a3b273",
            "https://www.linkedin.com/in/rezaul-karim-803a3b273"));

        card.add(photo);
        card.add(Box.createHorizontalStrut(18));
        card.add(textCol);
        card.add(Box.createHorizontalGlue());
        return card;
    }

    /** One small contact row on the Leadership card: an icon glyph + text. If {@code url} is
     *  non-null the text is styled as a link and clicking it opens the URL in the system's
     *  default browser via {@link Desktop} (falls back to plain text if Desktop isn't
     *  supported in this environment). */
    private JComponent contactLine(String iconGlyph, String text, String url) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(iconGlyph + "  ");
        icon.setFont(icon.getFont().deriveFont(Font.PLAIN, 12f));
        icon.setForeground(new Color(120, 100, 90));
        row.add(icon);

        JLabel value = new JLabel(text);
        value.setFont(value.getFont().deriveFont(Font.PLAIN, 12f));
        if (url != null) {
            value.setForeground(new Color(30, 90, 170));
            value.setText("<html><u>" + text + "</u></html>");
            value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            value.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new java.net.URI(url));
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AboutPanel.this,
                            "Couldn't open the link automatically. URL: " + url,
                            "Open Link", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
        } else {
            value.setForeground(new Color(90, 75, 65));
        }
        row.add(value);
        return row;
    }

    /** Loads an image from the classpath, crops it to a square, scales it, and clips it to a
     *  circle for a clean avatar look. Returns null (caller falls back to a placeholder) if the
     *  resource isn't found or isn't readable, rather than throwing. */
    private ImageIcon loadCircularAvatar(String resourcePath, int size) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            BufferedImage original = ImageIO.read(is);
            if (original == null) return null;

            int w = original.getWidth(), h = original.getHeight();
            int side = Math.min(w, h);
            BufferedImage square = original.getSubimage((w - side) / 2, (h - side) / 2, side, side);
            Image scaled = square.getScaledInstance(size, size, Image.SCALE_SMOOTH);

            BufferedImage circular = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = circular.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(new Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();
            return new ImageIcon(circular);
        } catch (IOException e) {
            return null;
        }
    }

    private JPanel buildMissionVisionRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(520, 150));
        row.add(missionVisionCard("Our Mission",
            "Brew honest, high-quality coffee and food for our neighborhood, serve every " +
            "customer like family, and give our team a place to grow — one cup at a time.",
            UITheme.NAV_ABOUT));
        row.add(missionVisionCard("Our Vision",
            "To be the coffee shop New Yorkers can't imagine their block without — known for " +
            "quality, community, and consistency, from the first order to the last.",
            UITheme.NAV_RECIPES));
        return row;
    }

    private JPanel missionVisionCard(String title, String text, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setForeground(accent.darker());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea body = new JTextArea(text);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(false);
        body.setOpaque(false);
        body.setFont(body.getFont().deriveFont(Font.PLAIN, 12f));
        body.setForeground(new Color(70, 55, 45));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(body);
        return card;
    }

    private JPanel buildStatsGrid() {
        int employeeCount = 0;
        int menuItemCount = 0;
        int supplierCount = 0;
        try { employeeCount = new EmployeeDAO().getAllEmployees().size(); } catch (RuntimeException ignored) { }
        try { menuItemCount = new MenuItemDAO().getAllMenuItems().size(); } catch (RuntimeException ignored) { }
        try { supplierCount = new SupplierDAO().getAllSuppliers().size(); } catch (RuntimeException ignored) { }

        JPanel grid = new JPanel(new GridLayout(2, 2, 24, 10));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(480, 90));
        grid.add(statCard("Employees on File", String.valueOf(employeeCount)));
        grid.add(statCard("Menu Items", String.valueOf(menuItemCount)));
        grid.add(statCard("Active Suppliers", String.valueOf(supplierCount)));
        grid.add(statCard("Today", LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
        return grid;
    }

    private JPanel statCard(String label, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 210, 190)),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 20f));
        valueLabel.setForeground(new Color(214, 80, 40));

        JLabel captionLabel = new JLabel(label);
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        captionLabel.setForeground(new Color(110, 95, 85));

        card.add(valueLabel);
        card.add(captionLabel);
        return card;
    }

    private JPanel supportLine(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(label + ":  ");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setForeground(new Color(70, 55, 45));
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.PLAIN, 12f));
        v.setForeground(new Color(90, 75, 65));
        row.add(l);
        row.add(v);
        return row;
    }
}
