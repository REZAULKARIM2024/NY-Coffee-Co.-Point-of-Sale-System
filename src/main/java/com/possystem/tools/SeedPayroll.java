package com.possystem.tools;

import com.possystem.config.DBConnection;
import com.possystem.dao.EmployeeDAO;
import com.possystem.model.Employee;
import com.possystem.model.PayrollRun;
import com.possystem.service.PayrollService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One-off seeding utility: generates 8 weeks of realistic time_clock punches for every
 * employee (except the owner, who doesn't take an hourly wage), then runs actual payroll
 * (via the same PayrollService the UI's "Run Payroll..." button uses) for 4 consecutive
 * bi-weekly periods per employee, so the Payroll screen has a full, realistic history.
 *
 * Usage: java -cp target/classes;lib/mysql-connector-j-*.jar com.possystem.tools.SeedPayroll
 */
public class SeedPayroll {

    private static final Random RND = new Random(99);
    private static final String[] PAYOUT_METHODS = {"DIRECT_DEPOSIT", "DIRECT_DEPOSIT", "DIRECT_DEPOSIT", "CASH", "CHECK"};

    public static void main(String[] args) throws Exception {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusWeeks(8).plusDays(1);
        System.out.println("Seeding time clock data from " + start + " to " + end);

        ensureHolidays();

        EmployeeDAO employeeDAO = new EmployeeDAO();
        List<Employee> all = employeeDAO.getAllEmployees();
        List<Employee> workers = new ArrayList<>();
        for (Employee e : all) {
            if (!"Owner and Managing Director".equals(e.getPosition())) workers.add(e);
        }
        System.out.println("Employees to seed: " + workers.size() + " (excluded owner)");

        int shiftsInserted = insertTimeClockShifts(workers, start, end);
        System.out.println("Time clock shifts inserted: " + shiftsInserted);

        int runsCreated = runBiweeklyPayroll(workers, start, end);
        System.out.println("Payroll runs created: " + runsCreated);

        System.out.println("DONE.");
    }

    private static void ensureHolidays() throws Exception {
        String[][] holidays = {
            {"2026-06-19", "Juneteenth"},
            {"2026-07-04", "Independence Day"}
        };
        try (Connection conn = DBConnection.getConnection()) {
            for (String[] h : holidays) {
                String checkSql = "SELECT 1 FROM holidays WHERE holiday_date = ?";
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setDate(1, Date.valueOf(h[0]));
                    if (check.executeQuery().next()) continue;
                }
                String insertSql = "INSERT INTO holidays (holiday_date, description) VALUES (?, ?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    ins.setDate(1, Date.valueOf(h[0]));
                    ins.setString(2, h[1]);
                    ins.executeUpdate();
                }
            }
        }
        System.out.println("Holidays ensured.");
    }

    private static int insertTimeClockShifts(List<Employee> workers, LocalDate start, LocalDate end) throws Exception {
        String sql = "INSERT INTO time_clock (employee_id, clock_in, clock_out) VALUES (?, ?, ?)";
        int count = 0;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Employee emp : workers) {
                    // Each employee gets 1-2 fixed regular days off per week, for shift-pattern variety.
                    int dayOff1 = RND.nextInt(7);
                    int dayOff2 = RND.nextBoolean() ? RND.nextInt(7) : -1;

                    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                        int dow = d.getDayOfWeek().getValue() % 7; // 0=Sun..6=Sat roughly via getValue (Mon=1..Sun=7)%7 -> Sun=0
                        boolean isRegularOff = dow == dayOff1 || dow == dayOff2;

                        boolean works;
                        if (isRegularOff) {
                            works = RND.nextDouble() < 0.08; // rare pickup shift on a normal day off
                        } else {
                            works = RND.nextDouble() < 0.90; // occasional unplanned day off
                        }
                        if (!works) continue;

                        double r = RND.nextDouble();
                        double hours;
                        if (r < 0.15) hours = 9 + RND.nextDouble();       // long shift -> feeds overtime
                        else if (r < 0.25) hours = 4 + RND.nextDouble();  // short/split shift
                        else hours = 7 + RND.nextDouble();                // typical shift

                        int startHour = 6 + RND.nextInt(9); // 6am - 2pm start window
                        int startMinute = RND.nextBoolean() ? 0 : 30;
                        LocalDateTime clockIn = LocalDateTime.of(d, LocalTime.of(startHour, startMinute));
                        LocalDateTime clockOut = clockIn.plusMinutes((long) Math.round(hours * 60));

                        ps.setInt(1, emp.getId());
                        ps.setTimestamp(2, java.sql.Timestamp.valueOf(clockIn));
                        ps.setTimestamp(3, java.sql.Timestamp.valueOf(clockOut));
                        ps.addBatch();
                        count++;

                        if (count % 500 == 0) {
                            ps.executeBatch();
                            conn.commit();
                        }
                    }
                }
                ps.executeBatch();
                conn.commit();
            }
            conn.setAutoCommit(true);
        }
        return count;
    }

    private static int runBiweeklyPayroll(List<Employee> workers, LocalDate start, LocalDate end) {
        PayrollService payrollService = new PayrollService();
        int runs = 0;
        LocalDate periodStart = start;
        while (!periodStart.isAfter(end)) {
            LocalDate periodEnd = periodStart.plusDays(13);
            if (periodEnd.isAfter(end)) periodEnd = end;

            for (Employee emp : workers) {
                BigDecimal deductions = BigDecimal.valueOf(20 + RND.nextInt(60))
                        .setScale(2, RoundingMode.HALF_UP);
                String method = PAYOUT_METHODS[RND.nextInt(PAYOUT_METHODS.length)];
                try {
                    PayrollRun run = payrollService.runPayroll(emp, Date.valueOf(periodStart), Date.valueOf(periodEnd),
                            deductions, method);
                    if (run != null) runs++;
                } catch (RuntimeException ex) {
                    System.out.println("Skipped " + emp.getFullName() + " (" + periodStart + " - " + periodEnd + "): " + ex.getMessage());
                }
            }
            periodStart = periodEnd.plusDays(1);
        }
        return runs;
    }
}
