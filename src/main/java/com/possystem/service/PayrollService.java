package com.possystem.service;

import com.possystem.dao.PayrollDAO;
import com.possystem.model.Employee;
import com.possystem.model.PayrollRun;
import com.possystem.model.TimeClockEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns raw time_clock punches into a payroll run with hours split into
 * regular / overtime / weekend / holiday buckets, each paid at its own multiplier.
 *
 * Classification rules (in priority order, per shift):
 *   1. Shift date is in the `holidays` table         -> holiday hours (HOLIDAY_MULTIPLIER)
 *   2. Shift date falls on Sat/Sun                    -> weekend hours (WEEKEND_MULTIPLIER)
 *   3. Otherwise                                       -> weekday hours, bucketed by ISO week;
 *      within each week, the first WEEKLY_OVERTIME_THRESHOLD_HOURS are regular pay,
 *      anything beyond that in the same week is overtime (OVERTIME_MULTIPLIER).
 *
 * Only closed shifts (clock_out set) within [periodStart, periodEnd] are counted.
 */
public class PayrollService {

    public static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.5");
    public static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.25");
    public static final BigDecimal HOLIDAY_MULTIPLIER = new BigDecimal("1.5");
    public static final BigDecimal WEEKLY_OVERTIME_THRESHOLD_HOURS = new BigDecimal("40");

    // Simplified flat-rate withholding approximation applied to gross pay for the printed
    // paystub. Social Security and Medicare use the real fixed FICA rates; federal/state/city
    // are flat approximations of typical effective withholding (not a true bracket calculation).
    public static final BigDecimal FEDERAL_TAX_RATE = new BigDecimal("0.0703");
    public static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.0620");
    public static final BigDecimal MEDICARE_RATE = new BigDecimal("0.0145");
    public static final BigDecimal NY_STATE_TAX_RATE = new BigDecimal("0.0399");
    public static final BigDecimal NYC_CITY_TAX_RATE = new BigDecimal("0.0293");
    public static final int DEFAULT_DAYS_TO_PAY_DATE = 6;

    private final PayrollDAO payrollDAO = new PayrollDAO();

    /** Computes hours/pay for the period and persists the resulting payroll run. */
    public PayrollRun runPayroll(Employee employee, Date periodStart, Date periodEnd,
                                  BigDecimal deductions, String payoutMethod) {
        List<TimeClockEntry> entries = payrollDAO.getEntriesForPeriod(employee.getId(), periodStart, periodEnd);

        BigDecimal weekendHours = BigDecimal.ZERO;
        BigDecimal holidayHours = BigDecimal.ZERO;
        Map<String, BigDecimal> weekdayHoursByWeek = new LinkedHashMap<>();

        for (TimeClockEntry entry : entries) {
            if (entry.getClockOut() == null) continue; // still clocked in, don't count a partial shift

            LocalDateTime in = entry.getClockIn().toLocalDateTime();
            LocalDateTime out = entry.getClockOut().toLocalDateTime();
            BigDecimal hours = BigDecimal.valueOf(Duration.between(in, out).toMinutes())
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            if (hours.signum() <= 0) continue;

            LocalDate shiftDate = in.toLocalDate();
            DayOfWeek dow = shiftDate.getDayOfWeek();

            if (payrollDAO.isHoliday(Date.valueOf(shiftDate))) {
                holidayHours = holidayHours.add(hours);
            } else if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendHours = weekendHours.add(hours);
            } else {
                WeekFields iso = WeekFields.ISO;
                String weekKey = shiftDate.get(iso.weekBasedYear()) + "-W" + shiftDate.get(iso.weekOfWeekBasedYear());
                weekdayHoursByWeek.merge(weekKey, hours, BigDecimal::add);
            }
        }

        BigDecimal regularHours = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        for (BigDecimal weekTotal : weekdayHoursByWeek.values()) {
            if (weekTotal.compareTo(WEEKLY_OVERTIME_THRESHOLD_HOURS) > 0) {
                regularHours = regularHours.add(WEEKLY_OVERTIME_THRESHOLD_HOURS);
                overtimeHours = overtimeHours.add(weekTotal.subtract(WEEKLY_OVERTIME_THRESHOLD_HOURS));
            } else {
                regularHours = regularHours.add(weekTotal);
            }
        }

        BigDecimal rate = employee.getHourlyRate() == null ? BigDecimal.ZERO : employee.getHourlyRate();
        BigDecimal gross = computeGrossPay(regularHours, overtimeHours, weekendHours, holidayHours, rate);

        BigDecimal ded = deductions == null ? BigDecimal.ZERO : deductions;

        BigDecimal[] taxes = computeTaxes(gross);
        BigDecimal federalTax = taxes[0];
        BigDecimal socialSecurity = taxes[1];
        BigDecimal medicare = taxes[2];
        BigDecimal stateTax = taxes[3];
        BigDecimal cityTax = taxes[4];
        BigDecimal totalTaxes = federalTax.add(socialSecurity).add(medicare).add(stateTax).add(cityTax);

        BigDecimal net = gross.subtract(totalTaxes).subtract(ded);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
        net = net.setScale(2, RoundingMode.HALF_UP);

        PayrollRun run = new PayrollRun();
        run.setEmployeeId(employee.getId());
        run.setEmployeeName(employee.getFullName());
        run.setPeriodStart(periodStart);
        run.setPeriodEnd(periodEnd);
        run.setRegularHours(regularHours.setScale(2, RoundingMode.HALF_UP));
        run.setOvertimeHours(overtimeHours.setScale(2, RoundingMode.HALF_UP));
        run.setWeekendHours(weekendHours.setScale(2, RoundingMode.HALF_UP));
        run.setHolidayHours(holidayHours.setScale(2, RoundingMode.HALF_UP));
        run.setDeductions(ded.setScale(2, RoundingMode.HALF_UP));
        run.setGrossPay(gross);
        run.setNetPay(net);
        run.setFederalTax(federalTax);
        run.setSocialSecurity(socialSecurity);
        run.setMedicare(medicare);
        run.setStateTax(stateTax);
        run.setCityTax(cityTax);
        run.setPayDate(Date.valueOf(periodEnd.toLocalDate().plusDays(DEFAULT_DAYS_TO_PAY_DATE)));
        run.setPayoutMethod(payoutMethod);
        run.setPaymentReference(payoutMethod + "-PR-" + System.currentTimeMillis());

        payrollDAO.savePayrollRun(run);
        return run;
    }

    /**
     * Pure gross-pay formula, pulled out of runPayroll() so it can be unit tested without a
     * database: regular hours at the base rate, plus overtime/weekend/holiday hours at their
     * respective multipliers, rounded to cents.
     */
    public static BigDecimal computeGrossPay(BigDecimal regularHours, BigDecimal overtimeHours,
                                              BigDecimal weekendHours, BigDecimal holidayHours, BigDecimal rate) {
        BigDecimal r = rate == null ? BigDecimal.ZERO : rate;
        return nz(regularHours).multiply(r)
                .add(nz(overtimeHours).multiply(r).multiply(OVERTIME_MULTIPLIER))
                .add(nz(weekendHours).multiply(r).multiply(WEEKEND_MULTIPLIER))
                .add(nz(holidayHours).multiply(r).multiply(HOLIDAY_MULTIPLIER))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Pure withholding formula, pulled out of runPayroll() so it can be unit tested without a
     * database. Returns {federalTax, socialSecurity, medicare, stateTax, cityTax}, each rounded
     * to cents, in that fixed order.
     */
    public static BigDecimal[] computeTaxes(BigDecimal gross) {
        BigDecimal g = nz(gross);
        return new BigDecimal[]{
            g.multiply(FEDERAL_TAX_RATE).setScale(2, RoundingMode.HALF_UP),
            g.multiply(SOCIAL_SECURITY_RATE).setScale(2, RoundingMode.HALF_UP),
            g.multiply(MEDICARE_RATE).setScale(2, RoundingMode.HALF_UP),
            g.multiply(NY_STATE_TAX_RATE).setScale(2, RoundingMode.HALF_UP),
            g.multiply(NYC_CITY_TAX_RATE).setScale(2, RoundingMode.HALF_UP)
        };
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
