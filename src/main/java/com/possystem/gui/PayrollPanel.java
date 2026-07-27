package com.possystem.gui;

import com.possystem.dao.EmployeeDAO;
import com.possystem.dao.PayrollDAO;
import com.possystem.model.Employee;
import com.possystem.model.PayrollRun;
import com.possystem.service.PayrollService;
import com.possystem.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Manager/admin tool: pick an employee + pay period, calculate hours (regular/overtime/
 * weekend/holiday) from their time clock punches via PayrollService, and save the run.
 */
public class PayrollPanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final PayrollDAO payrollDAO = new PayrollDAO();
    private final PayrollService payrollService = new PayrollService();

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Employee", "Period", "Reg Hrs", "OT Hrs", "Weekend Hrs", "Holiday Hrs", "Deductions",
            "Gross", "Taxes", "Net", "Pay Date", "Check #", "Method"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private List<PayrollRun> currentRuns = new java.util.ArrayList<>();
    private final JButton paystubBtn = UITheme.styledButton("Print Paystub...", UITheme.NAV_RECIPES);
    private final JButton paycheckBtn = UITheme.styledButton("Print Paycheck...", UITheme.NAV_EMPLOYEES);

    public PayrollPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.SIDEBAR_BG);

        UITheme.styleTable(table, UITheme.NAV_PAYROLL);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                paystubBtn.setEnabled(hasSelection);
                paycheckBtn.setEnabled(hasSelection);
            }
        });

        JButton runBtn = UITheme.styledButton("Run Payroll...", UITheme.NAV_PAYROLL);
        runBtn.addActionListener(e -> openRunDialog());
        paystubBtn.setEnabled(false);
        paystubBtn.addActionListener(e -> openPaystub());
        paycheckBtn.setEnabled(false);
        paycheckBtn.addActionListener(e -> openPaycheck());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.add(runBtn);
        buttons.add(paystubBtn);
        buttons.add(paycheckBtn);
        add(buttons, BorderLayout.SOUTH);

        loadTable();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        try {
            currentRuns = payrollDAO.getAllPayrollRuns();
            for (PayrollRun r : currentRuns) {
                tableModel.addRow(new Object[]{
                    r.getEmployeeName(),
                    r.getPeriodStart() + " to " + r.getPeriodEnd(),
                    r.getRegularHours(), r.getOvertimeHours(), r.getWeekendHours(), r.getHolidayHours(),
                    r.getDeductions(), r.getGrossPay(), r.getTotalTaxes(), r.getNetPay(),
                    r.getPayDate(), r.getCheckNumber(), r.getPayoutMethod()
                });
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Failed to load payroll history: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openPaystub() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentRuns.size()) return;
        PayrollRun run = currentRuns.get(row);
        Employee employee = null;
        for (Employee e : employeeDAO.getAllEmployees()) {
            if (e.getId() == run.getEmployeeId()) { employee = e; break; }
        }
        if (employee == null) {
            JOptionPane.showMessageDialog(this, "Could not find the employee record for this payroll run.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BigDecimal[] ytd = payrollDAO.getYtdTotals(run.getEmployeeId(), run.getPeriodEnd());
        Window owner = SwingUtilities.getWindowAncestor(this);
        new PaystubDialog(owner, run, employee, ytd).setVisible(true);
    }

    private void openPaycheck() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentRuns.size()) return;
        PayrollRun run = currentRuns.get(row);
        Employee employee = null;
        for (Employee e : employeeDAO.getAllEmployees()) {
            if (e.getId() == run.getEmployeeId()) { employee = e; break; }
        }
        if (employee == null) {
            JOptionPane.showMessageDialog(this, "Could not find the employee record for this payroll run.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        new PaycheckDialog(owner, run, employee).setVisible(true);
    }

    private void openRunDialog() {
        List<Employee> employees = employeeDAO.getAllEmployees();
        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No employees exist yet. Add one under Employees first.");
            return;
        }

        JComboBox<Employee> employeeBox = new JComboBox<>(employees.toArray(new Employee[0]));
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);
        JTextField startField = new JTextField(weekAgo.toString());
        JTextField endField = new JTextField(today.toString());
        JTextField deductionsField = new JTextField("0.00");
        String[] methods = {"DIRECT_DEPOSIT", "CASH", "CHECK"};
        JComboBox<String> methodBox = new JComboBox<>(methods);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Employee:")); form.add(employeeBox);
        form.add(new JLabel("Period Start (yyyy-mm-dd):")); form.add(startField);
        form.add(new JLabel("Period End (yyyy-mm-dd):")); form.add(endField);
        form.add(new JLabel("Deductions:")); form.add(deductionsField);
        form.add(new JLabel("Payout Method:")); form.add(methodBox);

        int result = JOptionPane.showConfirmDialog(this, form, "Run Payroll",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        LocalDate start, end;
        try {
            start = LocalDate.parse(startField.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            end = LocalDate.parse(endField.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be in yyyy-mm-dd format.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (end.isBefore(start)) {
            JOptionPane.showMessageDialog(this, "Period end must be on or after period start.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal deductions;
        try {
            deductions = new BigDecimal(deductionsField.getText().trim());
            if (deductions.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Deductions must be a non-negative number.", "Validation error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee employee = (Employee) employeeBox.getSelectedItem();
        try {
            PayrollRun run = payrollService.runPayroll(employee, Date.valueOf(start), Date.valueOf(end),
                    deductions, (String) methodBox.getSelectedItem());
            JOptionPane.showMessageDialog(this,
                "Payroll run saved for " + employee.getFullName() + ".\n" +
                "Regular: " + run.getRegularHours() + " hrs, Overtime: " + run.getOvertimeHours() + " hrs, " +
                "Weekend: " + run.getWeekendHours() + " hrs, Holiday: " + run.getHolidayHours() + " hrs\n" +
                "Gross: $" + run.getGrossPay() + "   Deductions: $" + run.getDeductions() + "   Net: $" + run.getNetPay(),
                "Payroll complete", JOptionPane.INFORMATION_MESSAGE);
            loadTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Payroll run failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
