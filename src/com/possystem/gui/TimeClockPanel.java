package com.possystem.gui;

import com.possystem.dao.PayrollDAO;
import com.possystem.model.TimeClockEntry;
import com.possystem.model.User;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Self-service clock in/out for the logged-in employee. Managers/admins additionally
 * see a live feed of everyone's recent punches underneath.
 */
public class TimeClockPanel extends JPanel {

    private final PayrollDAO payrollDAO = new PayrollDAO();
    private final User currentUser;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy h:mm a");

    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton toggleBtn = new JButton("Clock In");

    private final DefaultTableModel allTableModel = new DefaultTableModel(
        new Object[]{"Employee", "Clock In", "Clock Out"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };

    public TimeClockPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        toggleBtn.setOpaque(true);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setForeground(Color.WHITE);
        toggleBtn.setFont(toggleBtn.getFont().deriveFont(Font.BOLD, 14f));

        add(buildSelfPanel(), BorderLayout.NORTH);

        if (currentUser.isManagerOrAbove()) {
            JPanel bottom = new JPanel(new BorderLayout(4, 4));
            bottom.setOpaque(false);
            JLabel bottomLabel = new JLabel("Recent punches — all employees");
            bottomLabel.setFont(bottomLabel.getFont().deriveFont(Font.BOLD, 13f));
            bottomLabel.setForeground(UITheme.NAV_TIMECLOCK.darker());
            bottom.add(bottomLabel, BorderLayout.NORTH);
            JTable allTable = new JTable(allTableModel);
            UITheme.styleTable(allTable, UITheme.NAV_TIMECLOCK);
            bottom.add(new JScrollPane(allTable), BorderLayout.CENTER);
            add(bottom, BorderLayout.CENTER);
            loadAllEntries();
        }

        refreshStatus();
    }

    private JComponent buildSelfPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("My Time Clock — " + currentUser.getFullName()));

        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(8));

        toggleBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        toggleBtn.setMaximumSize(new Dimension(200, 40));
        toggleBtn.addActionListener(e -> toggleClock());
        panel.add(toggleBtn);
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    private void refreshStatus() {
        if (currentUser.getEmployeeId() == null) {
            statusLabel.setText("This login isn't linked to an employee record — ask an admin to link one.");
            toggleBtn.setEnabled(false);
            return;
        }
        try {
            TimeClockEntry open = payrollDAO.getOpenEntry(currentUser.getEmployeeId());
            if (open != null) {
                statusLabel.setText("Clocked IN since " + fmt.format(open.getClockIn()));
                toggleBtn.setText("Clock Out");
                toggleBtn.setBackground(UITheme.ACCENT_RED);
            } else {
                statusLabel.setText("Currently clocked OUT.");
                toggleBtn.setText("Clock In");
                toggleBtn.setBackground(UITheme.NAV_RECIPES);
            }
        } catch (RuntimeException ex) {
            statusLabel.setText("Failed to load clock status: " + ex.getMessage());
        }
    }

    private void toggleClock() {
        if (currentUser.getEmployeeId() == null) return;
        try {
            TimeClockEntry open = payrollDAO.getOpenEntry(currentUser.getEmployeeId());
            if (open != null) {
                payrollDAO.clockOut(currentUser.getEmployeeId());
            } else {
                payrollDAO.clockIn(currentUser.getEmployeeId());
            }
            refreshStatus();
            if (currentUser.isManagerOrAbove()) loadAllEntries();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAllEntries() {
        allTableModel.setRowCount(0);
        try {
            List<TimeClockEntry> entries = payrollDAO.getRecentEntries(50);
            for (TimeClockEntry t : entries) {
                allTableModel.addRow(new Object[]{
                    t.getEmployeeName(),
                    fmt.format(t.getClockIn()),
                    t.getClockOut() != null ? fmt.format(t.getClockOut()) : "(still clocked in)"
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load punches: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
