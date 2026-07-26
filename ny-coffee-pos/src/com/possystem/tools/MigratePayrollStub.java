package com.possystem.tools;

import com.possystem.config.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * One-off migration: adds the itemized tax-withholding / pay-date / check-number columns to
 * payroll_runs (needed for the printable pay statement), then backfills any existing rows that
 * predate this change using the same flat-rate withholding approximation as PayrollService.
 * Idempotent - safe to run more than once.
 */
public class MigratePayrollStub {

    private static final BigDecimal FEDERAL_TAX_RATE = new BigDecimal("0.0703");
    private static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.0620");
    private static final BigDecimal MEDICARE_RATE = new BigDecimal("0.0145");
    private static final BigDecimal NY_STATE_TAX_RATE = new BigDecimal("0.0399");
    private static final BigDecimal NYC_CITY_TAX_RATE = new BigDecimal("0.0293");

    public static void main(String[] args) throws Exception {
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            addColumnIfMissing(conn, st, "federal_tax", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(conn, st, "social_security", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(conn, st, "medicare", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(conn, st, "state_tax", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(conn, st, "city_tax", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(conn, st, "pay_date", "DATE NULL");
            addColumnIfMissing(conn, st, "check_number", "VARCHAR(20) NULL");

            int backfilled = 0;
            String selectSql = "SELECT id, period_end, gross_pay, net_pay, deductions FROM payroll_runs " +
                                "WHERE pay_date IS NULL OR check_number IS NULL";
            try (Statement sel = conn.createStatement();
                 ResultSet rs = sel.executeQuery(selectSql)) {
                String updateSql = "UPDATE payroll_runs SET federal_tax=?, social_security=?, medicare=?, " +
                                    "state_tax=?, city_tax=?, net_pay=?, pay_date=?, check_number=? WHERE id=?";
                try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        java.sql.Date periodEnd = rs.getDate("period_end");
                        BigDecimal gross = rs.getBigDecimal("gross_pay");
                        BigDecimal deductions = rs.getBigDecimal("deductions");
                        if (gross == null) gross = BigDecimal.ZERO;
                        if (deductions == null) deductions = BigDecimal.ZERO;

                        BigDecimal federal = gross.multiply(FEDERAL_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal ss = gross.multiply(SOCIAL_SECURITY_RATE).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal medicare = gross.multiply(MEDICARE_RATE).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal state = gross.multiply(NY_STATE_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal city = gross.multiply(NYC_CITY_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal totalTax = federal.add(ss).add(medicare).add(state).add(city);
                        BigDecimal net = gross.subtract(totalTax).subtract(deductions);
                        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
                        net = net.setScale(2, RoundingMode.HALF_UP);

                        java.sql.Date payDate = periodEnd == null ? null :
                            java.sql.Date.valueOf(periodEnd.toLocalDate().plusDays(6));
                        String checkNumber = String.format("%07d", 1000000 + id);

                        up.setBigDecimal(1, federal);
                        up.setBigDecimal(2, ss);
                        up.setBigDecimal(3, medicare);
                        up.setBigDecimal(4, state);
                        up.setBigDecimal(5, city);
                        up.setBigDecimal(6, net);
                        up.setDate(7, payDate);
                        up.setString(8, checkNumber);
                        up.setInt(9, id);
                        up.executeUpdate();
                        backfilled++;
                    }
                }
            }
            System.out.println("OK: backfilled tax/pay-date/check-number on " + backfilled + " existing payroll_runs rows");
        }
        System.out.println("DONE.");
    }

    private static void addColumnIfMissing(Connection conn, Statement st, String column, String definition) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                     "AND table_name = 'payroll_runs' AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("SKIP: payroll_runs." + column + " already exists");
                    return;
                }
            }
        }
        st.executeUpdate("ALTER TABLE payroll_runs ADD COLUMN " + column + " " + definition);
        System.out.println("OK: added payroll_runs." + column);
    }
}
