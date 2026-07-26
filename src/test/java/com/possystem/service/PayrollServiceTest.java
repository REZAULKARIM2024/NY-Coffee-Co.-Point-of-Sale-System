package com.possystem.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the pure payroll math extracted from PayrollService.runPayroll():
 * computeGrossPay() and computeTaxes(). Both are static and DB-free.
 */
class PayrollServiceTest {

    @Test
    void computeGrossPay_regularHoursOnly() {
        BigDecimal gross = PayrollService.computeGrossPay(
                new BigDecimal("40"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20.00"));
        assertEquals(new BigDecimal("800.00"), gross);
    }

    @Test
    void computeGrossPay_withOvertime_appliesOneAndHalfMultiplier() {
        // 40 regular + 5 OT @ $20/hr => 40*20 + 5*20*1.5 = 800 + 150 = 950
        BigDecimal gross = PayrollService.computeGrossPay(
                new BigDecimal("40"), new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20.00"));
        assertEquals(new BigDecimal("950.00"), gross);
    }

    @Test
    void computeGrossPay_withWeekendHours_appliesOneQuarterPremium() {
        // 8 weekend hours @ $15/hr * 1.25 = 150.00
        BigDecimal gross = PayrollService.computeGrossPay(
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("8"), BigDecimal.ZERO,
                new BigDecimal("15.00"));
        assertEquals(new BigDecimal("150.00"), gross);
    }

    @Test
    void computeGrossPay_withHolidayHours_appliesOneAndHalfMultiplier() {
        // 8 holiday hours @ $18/hr * 1.5 = 216.00
        BigDecimal gross = PayrollService.computeGrossPay(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("8"),
                new BigDecimal("18.00"));
        assertEquals(new BigDecimal("216.00"), gross);
    }

    @Test
    void computeGrossPay_allBucketsCombined() {
        // 40 reg + 5 OT + 8 weekend + 4 holiday @ $20/hr
        // = 800 + (5*20*1.5=150) + (8*20*1.25=200) + (4*20*1.5=120) = 1270.00
        BigDecimal gross = PayrollService.computeGrossPay(
                new BigDecimal("40"), new BigDecimal("5"), new BigDecimal("8"), new BigDecimal("4"),
                new BigDecimal("20.00"));
        assertEquals(new BigDecimal("1270.00"), gross);
    }

    @Test
    void computeGrossPay_nullInputs_treatedAsZero() {
        BigDecimal gross = PayrollService.computeGrossPay(null, null, null, null, new BigDecimal("20.00"));
        assertEquals(new BigDecimal("0.00"), gross);
    }

    @Test
    void computeGrossPay_nullRate_treatedAsZero() {
        BigDecimal gross = PayrollService.computeGrossPay(new BigDecimal("40"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null);
        assertEquals(new BigDecimal("0.00"), gross);
    }

    @Test
    void computeTaxes_returnsFiveRatesInFixedOrder() {
        BigDecimal[] taxes = PayrollService.computeTaxes(new BigDecimal("1000.00"));
        assertEquals(5, taxes.length);
        assertEquals(new BigDecimal("70.30"), taxes[0]); // federal 7.03%
        assertEquals(new BigDecimal("62.00"), taxes[1]); // social security 6.20%
        assertEquals(new BigDecimal("14.50"), taxes[2]); // medicare 1.45%
        assertEquals(new BigDecimal("39.90"), taxes[3]); // NY state 3.99%
        assertEquals(new BigDecimal("29.30"), taxes[4]); // NYC city 2.93%
    }

    @Test
    void computeTaxes_zeroGross_allZero() {
        BigDecimal[] taxes = PayrollService.computeTaxes(BigDecimal.ZERO);
        for (BigDecimal t : taxes) {
            assertEquals(new BigDecimal("0.00"), t);
        }
    }

    @Test
    void computeTaxes_nullGross_treatedAsZero() {
        BigDecimal[] taxes = PayrollService.computeTaxes(null);
        for (BigDecimal t : taxes) {
            assertEquals(new BigDecimal("0.00"), t);
        }
    }
}
