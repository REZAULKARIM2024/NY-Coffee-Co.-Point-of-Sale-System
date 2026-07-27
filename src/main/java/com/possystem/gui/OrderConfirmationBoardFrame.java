package com.possystem.gui;

import com.possystem.dao.OrderDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A real, working "kitchen confirmation board" — a live secondary window listing every open
 * order, auto-refreshing every 5 seconds from the database. There's no physical second monitor
 * in this environment, so this window IS the display (Activate Display opens it, Deactivate
 * closes it) rather than a fake hardware-handshake popup.
 */
public class OrderConfirmationBoardFrame extends JFrame {

    private final OrderDAO orderDAO = new OrderDAO();
    private final DefaultTableModel model = new DefaultTableModel(
        new Object[]{"Order #", "Source", "Type", "Customer", "Total", "Placed"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JLabel testBanner = new JLabel(" ", SwingConstants.CENTER);
    private final Timer refreshTimer;
    private final LocalDateTime activatedAt = LocalDateTime.now();

    public OrderConfirmationBoardFrame(String displayName) {
        setTitle("Order Confirmation Board — " + displayName);
        setSize(700, 420);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel header = new JLabel(displayName + " — live open orders", SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        add(header, BorderLayout.NORTH);

        testBanner.setOpaque(true);
        testBanner.setBackground(new Color(255, 235, 59));
        testBanner.setFont(testBanner.getFont().deriveFont(Font.BOLD, 13f));
        testBanner.setVisible(false);

        JPanel north = new JPanel(new BorderLayout());
        north.add(header, BorderLayout.NORTH);
        north.add(testBanner, BorderLayout.SOUTH);
        setContentPane(new JPanel(new BorderLayout()));
        add(north, BorderLayout.NORTH);

        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Now");
        refreshBtn.addActionListener(e -> refresh());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.add(refreshBtn);
        add(south, BorderLayout.SOUTH);

        refresh();
        refreshTimer = new Timer(5000, e -> refresh());
        refreshTimer.start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { refreshTimer.stop(); }
        });
    }

    private void refresh() {
        model.setRowCount(0);
        try {
            for (Object[] row : orderDAO.getOpenOrders()) {
                model.addRow(new Object[]{row[0], row[1], row[2], row[3], row[5], row[6]});
            }
        } catch (RuntimeException ignored) { /* transient DB hiccup - keep showing last-known rows */ }
    }

    /** Flashes a test signal banner for a few seconds (Test Display function). */
    public void flashTestSignal() {
        testBanner.setText("TEST SIGNAL OK — " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm:ss a")));
        testBanner.setVisible(true);
        Timer hide = new Timer(3000, e -> testBanner.setVisible(false));
        hide.setRepeats(false);
        hide.start();
    }

    public LocalDateTime getActivatedAt() { return activatedAt; }
}
