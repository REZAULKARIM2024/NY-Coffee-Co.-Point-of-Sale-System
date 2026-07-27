package com.possystem.dao;

import com.possystem.config.DBConnection;
import com.possystem.model.PayrollRun;
import com.possystem.model.TimeClockEntry;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollDAO {

    // ---------- TIME CLOCK ----------

    /** Returns the employee's currently-open (not clocked out) entry, or null if not clocked in. */
    public TimeClockEntry getOpenEntry(int employeeId) {
        String sql = "SELECT * FROM time_clock WHERE employee_id = ? AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check clock status: " + e.getMessage(), e);
        }
        return null;
    }

    public void clockIn(int employeeId) {
        if (getOpenEntry(employeeId) != null) {
            throw new RuntimeException("Already clocked in.");
        }
        String sql = "INSERT INTO time_clock (employee_id, clock_in) VALUES (?, NOW())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Clock-in failed: " + e.getMessage(), e);
        }
    }

    public void clockOut(int employeeId) {
        TimeClockEntry open = getOpenEntry(employeeId);
        if (open == null) {
            throw new RuntimeException("Not currently clocked in.");
        }
        String sql = "UPDATE time_clock SET clock_out = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, open.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Clock-out failed: " + e.getMessage(), e);
        }
    }

    /** All time_clock entries for one employee whose clock_in falls within [start, end] (inclusive dates), most recent first. */
    public List<TimeClockEntry> getEntriesForPeriod(int employeeId, Date start, Date end) {
        List<TimeClockEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM time_clock WHERE employee_id = ? AND DATE(clock_in) BETWEEN ? AND ? " +
                     "AND clock_out IS NOT NULL ORDER BY clock_in";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, start);
            ps.setDate(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load time clock entries: " + e.getMessage(), e);
        }
        return list;
    }

    /** Recent entries across all employees (joined with employee name), most recent first — for a manager overview. */
    public List<TimeClockEntry> getRecentEntries(int limit) {
        List<TimeClockEntry> list = new ArrayList<>();
        String sql = "SELECT tc.*, e.full_name FROM time_clock tc " +
                     "JOIN employees e ON tc.employee_id = e.id ORDER BY tc.clock_in DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TimeClockEntry t = mapRow(rs);
                    t.setEmployeeName(rs.getString("full_name"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load time clock entries: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean isHoliday(Date date) {
        String sql = "SELECT 1 FROM holidays WHERE holiday_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check holiday calendar: " + e.getMessage(), e);
        }
    }

    private TimeClockEntry mapRow(ResultSet rs) throws SQLException {
        TimeClockEntry t = new TimeClockEntry();
        t.setId(rs.getInt("id"));
        t.setEmployeeId(rs.getInt("employee_id"));
        t.setClockIn(rs.getTimestamp("clock_in"));
        t.setClockOut(rs.getTimestamp("clock_out"));
        return t;
    }

    // ---------- PAYROLL RUNS ----------

    public void savePayrollRun(PayrollRun run) {
        String sql = "INSERT INTO payroll_runs (employee_id, period_start, period_end, regular_hours, overtime_hours, " +
                     "weekend_hours, holiday_hours, deductions, gross_pay, net_pay, federal_tax, social_security, " +
                     "medicare, state_tax, city_tax, pay_date, check_number, payout_method, payment_reference) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, run.getEmployeeId());
            ps.setDate(2, run.getPeriodStart());
            ps.setDate(3, run.getPeriodEnd());
            ps.setBigDecimal(4, run.getRegularHours());
            ps.setBigDecimal(5, run.getOvertimeHours());
            ps.setBigDecimal(6, run.getWeekendHours());
            ps.setBigDecimal(7, run.getHolidayHours());
            ps.setBigDecimal(8, run.getDeductions());
            ps.setBigDecimal(9, run.getGrossPay());
            ps.setBigDecimal(10, run.getNetPay());
            ps.setBigDecimal(11, run.getFederalTax());
            ps.setBigDecimal(12, run.getSocialSecurity());
            ps.setBigDecimal(13, run.getMedicare());
            ps.setBigDecimal(14, run.getStateTax());
            ps.setBigDecimal(15, run.getCityTax());
            ps.setDate(16, run.getPayDate());
            ps.setString(17, run.getCheckNumber());
            ps.setString(18, run.getPayoutMethod());
            ps.setString(19, run.getPaymentReference());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) run.setId(keys.getInt(1));
            }
            if (run.getCheckNumber() == null) {
                String generated = String.format("%07d", 1000000 + run.getId());
                run.setCheckNumber(generated);
                updateCheckNumber(run.getId(), generated);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payroll run: " + e.getMessage(), e);
        }
    }

    public void updateCheckNumber(int runId, String checkNumber) {
        String sql = "UPDATE payroll_runs SET check_number = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, checkNumber);
            ps.setInt(2, runId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set check number: " + e.getMessage(), e);
        }
    }

    /**
     * Year-to-date totals for one employee, summing every payroll run whose period_end falls
     * in the same calendar year as (and on or before) throughDate. Index layout of the returned
     * array: [0]=gross, [1]=federalTax, [2]=socialSecurity, [3]=medicare, [4]=stateTax,
     * [5]=cityTax, [6]=deductions, [7]=net.
     */
    public BigDecimal[] getYtdTotals(int employeeId, Date throughDate) {
        String sql = "SELECT COALESCE(SUM(gross_pay),0), COALESCE(SUM(federal_tax),0), COALESCE(SUM(social_security),0), " +
                     "COALESCE(SUM(medicare),0), COALESCE(SUM(state_tax),0), COALESCE(SUM(city_tax),0), " +
                     "COALESCE(SUM(deductions),0), COALESCE(SUM(net_pay),0) " +
                     "FROM payroll_runs WHERE employee_id = ? AND YEAR(period_end) = YEAR(?) AND period_end <= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, throughDate);
            ps.setDate(3, throughDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal[] totals = new BigDecimal[8];
                    for (int i = 0; i < 8; i++) totals[i] = rs.getBigDecimal(i + 1);
                    return totals;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load YTD totals: " + e.getMessage(), e);
        }
        BigDecimal[] zeros = new BigDecimal[8];
        java.util.Arrays.fill(zeros, BigDecimal.ZERO);
        return zeros;
    }

    /** All payroll runs, most recent first, joined with employee name. */
    public List<PayrollRun> getAllPayrollRuns() {
        List<PayrollRun> list = new ArrayList<>();
        String sql = "SELECT pr.*, e.full_name FROM payroll_runs pr " +
                     "JOIN employees e ON pr.employee_id = e.id ORDER BY pr.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRunRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load payroll runs: " + e.getMessage(), e);
        }
        return list;
    }

    private PayrollRun mapRunRow(ResultSet rs) throws SQLException {
        PayrollRun r = new PayrollRun();
        r.setId(rs.getInt("id"));
        r.setEmployeeId(rs.getInt("employee_id"));
        r.setEmployeeName(rs.getString("full_name"));
        r.setPeriodStart(rs.getDate("period_start"));
        r.setPeriodEnd(rs.getDate("period_end"));
        r.setRegularHours(rs.getBigDecimal("regular_hours"));
        r.setOvertimeHours(rs.getBigDecimal("overtime_hours"));
        r.setWeekendHours(rs.getBigDecimal("weekend_hours"));
        r.setHolidayHours(rs.getBigDecimal("holiday_hours"));
        r.setDeductions(rs.getBigDecimal("deductions"));
        r.setGrossPay(rs.getBigDecimal("gross_pay"));
        r.setNetPay(rs.getBigDecimal("net_pay"));
        r.setFederalTax(nz(rs.getBigDecimal("federal_tax")));
        r.setSocialSecurity(nz(rs.getBigDecimal("social_security")));
        r.setMedicare(nz(rs.getBigDecimal("medicare")));
        r.setStateTax(nz(rs.getBigDecimal("state_tax")));
        r.setCityTax(nz(rs.getBigDecimal("city_tax")));
        r.setPayDate(rs.getDate("pay_date"));
        r.setCheckNumber(rs.getString("check_number"));
        r.setPayoutMethod(rs.getString("payout_method"));
        r.setPaymentReference(rs.getString("payment_reference"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
