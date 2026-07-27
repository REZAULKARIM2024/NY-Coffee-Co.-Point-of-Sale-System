package com.possystem.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the pure payroll math extracted from PayrollService.runPayroll():
 * computeGrossPay() and computeTaxes(). Both are static and DB-free.
 */
@Tag("unit")
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

    // ---------- Data-driven / boundary cases ----------

    @ParameterizedTest(name = "reg={0} ot={1} wknd={2} hol={3} rate={4} -> {5}")
    @CsvSource({
            "37.5, 0, 0, 0, 22.50, 843.75",
            "20, 10, 0, 0, 16.00, 560.00",
            "0, 0, 6.5, 0, 19.00, 154.38",   // 6.5*19=123.5, *1.25=154.375 -> rounds up to 154.38
            "0, 0, 0, 3.25, 14.00, 68.25",
            "0, 0, 0, 0, 20.00, 0.00"
    })
    void computeGrossPay_variousHourCombinations(String regular, String ot, String weekend, String holiday,
                                                  String rate, String expectedGross) {
        BigDecimal gross = PayrollService.computeGrossPay(
                new BigDecimal(regular), new BigDecimal(ot), new BigDecimal(weekend), new BigDecimal(holiday),
                new BigDecimal(rate));
        assertEquals(new BigDecimal(expectedGross), gross);
    }

    @ParameterizedTest(name = "gross={0} -> federal={1}")
    @CsvSource({
            "1000.00, 70.30",
            "500.00, 35.15",
            "0.00, 0.00",
            "123.45, 8.68",   // 123.45 * 0.0703 = 8.678535 -> rounds up to 8.68
            "50.00, 3.52"     // 50 * 0.0703 = 3.515 exactly -> HALF_UP rounds up to 3.52
    })
    void computeTaxes_federalRate_variousGrossAmounts(String gross, String expectedFederal) {
        BigDecimal[] taxes = PayrollService.computeTaxes(new BigDecimal(gross));
        assertEquals(new BigDecimal(expectedFederal), taxes[0]);
    }
}
