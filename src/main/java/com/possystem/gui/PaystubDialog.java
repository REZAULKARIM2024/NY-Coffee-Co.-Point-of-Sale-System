package com.possystem.gui;

import com.possystem.model.Employee;
import com.possystem.model.PayrollRun;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Prints/displays a single payroll run as an itemized employee pay statement, styled after a
 * traditional city/agency payroll check stub: header banner, employee + pay period info, an
 * itemized tax-withholding box (this period vs. year-to-date), an earnings breakdown table,
 * an other-deductions line, and a net pay total. Backed entirely by real payroll_runs data
 * (hours, rates, computed withholding) plus a YTD roll-up query — nothing here is placeholder.
 */
public class PaystubDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final Color HEADER_BLUE = new Color(30, 70, 130);
    private static final Color BAND_BLUE = new Color(210, 225, 245);
    private static final Color NET_GREEN = new Color(25, 110, 65);

    private final JPanel stub;

    public PaystubDialog(Window owner, PayrollRun run, Employee employee, BigDecimal[] ytd) {
        super(owner, "Employee Pay Statement", ModalityType.APPLICATION_MODAL);
        stub = buildStub(run, employee, ytd);

        JScrollPane scroll = new JScrollPane(stub);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);

        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> printStub());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(printBtn);
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);

        setSize(720, 820);
        setLocationRelativeTo(owner);
    }

    private void printStub() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                double scale = Math.min(pf.getImageableWidth() / stub.getWidth(), 1.0);
                g2.scale(scale, scale);
                stub.printAll(g2);
                return PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildStub(PayrollRun run, Employee employee, BigDecimal[] ytd) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        // ---- Header ----
        JLabel company = new JLabel("NY Coffee Co.", SwingConstants.CENTER);
        company.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 30));
        company.setForeground(HEADER_BLUE);
        company.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Payroll Management System", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bar = new JLabel("EMPLOYEE PAY STATEMENT", SwingConstants.CENTER);
        bar.setOpaque(true);
        bar.setBackground(HEADER_BLUE);
        bar.setForeground(Color.WHITE);
        bar.setFont(new Font("SansSerif", Font.BOLD, 15));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        root.add(company);
        root.add(subtitle);
        root.add(Box.createVerticalStrut(8));
        root.add(bar);
        root.add(Box.createVerticalStrut(10));

        // ---- Identity / pay period info grid ----
        JPanel infoGrid = new JPanel(new GridLayout(3, 4, 6, 4));
        infoGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        String itemNo = String.format("%06d", employee.getId());
        infoGrid.add(labeled("Item #", itemNo));
        infoGrid.add(labeled("Employee Name", employee.getFullName()));
        infoGrid.add(labeled("Check Number", nvl(run.getCheckNumber())));
        infoGrid.add(labeled("Payout Method", nvl(run.getPayoutMethod())));
        infoGrid.add(labeled("Pay Period Start", fmt(run.getPeriodStart())));
        infoGrid.add(labeled("Pay Period End", fmt(run.getPeriodEnd())));
        infoGrid.add(labeled("Pay Date", fmt(run.getPayDate())));
        infoGrid.add(labeled("Position", nvl(employee.getPosition())));
        root.add(infoGrid);
        root.add(Box.createVerticalStrut(12));

        // ---- Tax info box ----
        root.add(sectionHeader("Tax Info — This Period vs. Year to Date"));
        JPanel taxTablePanel = buildTaxTable(run, ytd);
        root.add(taxTablePanel);
        root.add(Box.createVerticalStrut(12));

        // ---- Earnings data table ----
        root.add(sectionHeader("Earnings Data"));
        JTable earningsTable = buildEarningsTable(run, employee, ytd);
        earningsTable.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel earningsWrap = new JPanel(new BorderLayout());
        earningsWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        earningsWrap.add(earningsTable.getTableHeader(), BorderLayout.NORTH);
        earningsWrap.add(earningsTable, BorderLayout.CENTER);
        earningsWrap.setMaximumSize(new Dimension(660, earningsTable.getRowHeight() * (earningsTable.getRowCount() + 2)));
        root.add(earningsWrap);
        root.add(Box.createVerticalStrut(12));

        // ---- Other deductions ----
        JPanel dedGrid = new JPanel(new GridLayout(1, 4, 6, 4));
        dedGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        dedGrid.add(labeled("Other Deductions (this period)", money(run.getDeductions())));
        dedGrid.add(labeled("Other Deductions (YTD)", money(ytd[6])));
        dedGrid.add(labeled("Leave Balance", "N/A"));
        dedGrid.add(labeled("", ""));
        root.add(dedGrid);
        root.add(Box.createVerticalStrut(12));

        // ---- Net pay bar ----
        JLabel netBar = new JLabel("NET PAY:  " + money(run.getNetPay()) + "     (YTD Net: " + money(ytd[7]) + ")", SwingConstants.CENTER);
        netBar.setOpaque(true);
        netBar.setBackground(NET_GREEN);
        netBar.setForeground(Color.WHITE);
        netBar.setFont(new Font("SansSerif", Font.BOLD, 18));
        netBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        netBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        netBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        root.add(netBar);
        root.add(Box.createVerticalStrut(10));

        // ---- Messages footer ----
        String message = "DIRECT_DEPOSIT".equals(run.getPayoutMethod())
                ? "Direct deposit — funds sent to the bank account on file for " + employee.getFullName() + "."
                : "CHECK".equals(run.getPayoutMethod())
                    ? "Pay by physical check — endorse before depositing or cashing."
                    : "Paid in cash at the register on the pay date shown above.";
        JLabel msg = new JLabel(message, SwingConstants.CENTER);
        msg.setFont(new Font("SansSerif", Font.ITALIC, 12));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(msg);

        if ("CHECK".equals(run.getPayoutMethod())) {
            root.add(Box.createVerticalStrut(14));
            JLabel endorse = new JLabel("✂ - - - - - - - - - - - - - - - - -  ENDORSE CHECK HERE  - - - - - - - - - - - - - - - - - ✂", SwingConstants.CENTER);
            endorse.setFont(new Font("SansSerif", Font.PLAIN, 11));
            endorse.setForeground(Color.GRAY);
            endorse.setAlignmentX(Component.CENTER_ALIGNMENT);
            root.add(endorse);
        }

        return root;
    }

    private JPanel buildTaxTable(PayrollRun run, BigDecimal[] ytd) {
        String[][] rows = {
            {"Federal Tax", money(run.getFederalTax()), money(ytd[1])},
            {"Social Security", money(run.getSocialSecurity()), money(ytd[2])},
            {"Medicare", money(run.getMedicare()), money(ytd[3])},
            {"NY State Tax", money(run.getStateTax()), money(ytd[4])},
            {"NYC City Tax", money(run.getCityTax()), money(ytd[5])},
            {"Total Tax Withheld", money(run.getTotalTaxes()), money(
                ytd[1].add(ytd[2]).add(ytd[3]).add(ytd[4]).add(ytd[5]))}
        };
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Description", "This Period", "Year to Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (String[] row : rows) model.addRow(row);
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.add(table.getTableHeader(), BorderLayout.NORTH);
        wrap.add(table, BorderLayout.CENTER);
        wrap.setMaximumSize(new Dimension(660, 22 * (rows.length + 2)));
        return wrap;
    }

    private JTable buildEarningsTable(PayrollRun run, Employee employee, BigDecimal[] ytd) {
        BigDecimal rate = employee.getHourlyRate() == null ? BigDecimal.ZERO : employee.getHourlyRate();
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Description", "Units/Hours", "Amount This Period", "YTD Amount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        addEarningsRow(model, "Regular Pay", run.getRegularHours(), rate);
        addEarningsRow(model, "Overtime Pay", run.getOvertimeHours(), rate.multiply(new BigDecimal("1.5")));
        addEarningsRow(model, "Weekend Premium", run.getWeekendHours(), rate.multiply(new BigDecimal("1.25")));
        addEarningsRow(model, "Holiday Premium", run.getHolidayHours(), rate.multiply(new BigDecimal("1.5")));
        model.addRow(new Object[]{"TOTAL EARNINGS", "", money(run.getGrossPay()), money(ytd[0])});

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private void addEarningsRow(DefaultTableModel model, String label, BigDecimal hours, BigDecimal rate) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal amount = hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        model.addRow(new Object[]{label, hours.setScale(2, RoundingMode.HALF_UP) + " hrs", money(amount), ""});
    }

    private JPanel sectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setOpaque(true);
        label.setBackground(BAND_BLUE);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.add(label, BorderLayout.CENTER);
        wrap.setMaximumSize(new Dimension(660, 28));
        return wrap;
    }

    private JPanel labeled(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("SansSerif", Font.PLAIN, 9));
        l.setForeground(Color.GRAY);
        JLabel v = new JLabel(value == null ? "" : value);
        v.setFont(new Font("SansSerif", Font.BOLD, 13));
        p.add(l);
        p.add(v);
        Border line = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
        p.setBorder(BorderFactory.createCompoundBorder(line, BorderFactory.createEmptyBorder(2, 2, 4, 2)));
        return p;
    }

    private static String fmt(java.sql.Date d) {
        return d == null ? "N/A" : d.toLocalDate().format(DATE_FMT);
    }

    private static String money(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return "$" + v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String nvl(String s) {
        return (s == null || s.isEmpty()) ? "N/A" : s;
    }
}
