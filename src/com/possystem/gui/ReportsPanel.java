package com.possystem.gui;

import com.possystem.dao.ReportDAO;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ReportsPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();
    private final JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
    private final DefaultTableModel topItemsModel = new DefaultTableModel(
        new Object[]{"Item", "Qty Sold Today", "Revenue"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UITheme.SIDEBAR_BG);

        JLabel title = new JLabel("Today's Sales Summary");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(UITheme.NAV_REPORTS.darker());

        statsRow.setOpaque(false);
        statsRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(statsRow, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.setOpaque(false);
        wrapper.add(top, BorderLayout.NORTH);

        JLabel topLabel = new JLabel("Top Sellers Today");
        topLabel.setFont(topLabel.getFont().deriveFont(Font.BOLD, 13f));
        topLabel.setForeground(UITheme.NAV_REPORTS.darker());

        JTable table = new JTable(topItemsModel);
        UITheme.styleTable(table, UITheme.NAV_REPORTS);
        JPanel tableWrap = new JPanel(new BorderLayout(4, 4));
        tableWrap.setOpaque(false);
        tableWrap.add(topLabel, BorderLayout.NORTH);
        tableWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        wrapper.add(tableWrap, BorderLayout.CENTER);

        JButton refreshBtn = UITheme.styledButton("Refresh", UITheme.NAV_REPORTS);
        refreshBtn.addActionListener(e -> loadData());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        south.add(refreshBtn);
        wrapper.add(south, BorderLayout.SOUTH);

        add(wrapper, BorderLayout.CENTER);

        loadData();
    }

    private void loadData() {
        try {
            ReportDAO.DailySummary summary = reportDAO.getTodaySummary();
            statsRow.removeAll();
            statsRow.add(statCard("Orders Today", String.valueOf(summary.orderCount), UITheme.NAV_REPORTS));
            statsRow.add(statCard("Total Sales", "$" + summary.totalSales, UITheme.NAV_RECIPES));
            statsRow.add(statCard("Tax Collected", "$" + summary.totalTax, UITheme.FEATURED_LEMONADE));
            statsRow.add(statCard("Discounts Given", "$" + summary.totalDiscount, UITheme.ACCENT_RED));
            statsRow.revalidate();
            statsRow.repaint();

            topItemsModel.setRowCount(0);
            for (ReportDAO.ItemSales item : reportDAO.getTopItemsToday(10)) {
                topItemsModel.addRow(new Object[]{item.itemName, item.quantitySold, "$" + item.revenue});
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel statCard(String label, String value, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 4, 0, accent),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        valueLabel.setForeground(accent);

        JLabel captionLabel = new JLabel(label);
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.PLAIN, 11f));
        captionLabel.setForeground(new Color(90, 90, 90));

        card.add(valueLabel);
        card.add(captionLabel);
        return card;
    }
}
