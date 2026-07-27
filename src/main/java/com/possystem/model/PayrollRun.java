package com.possystem.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class PayrollRun {
    private int id;
    private int employeeId;
    private String employeeName; // populated on joined reads
    private Date periodStart;
    private Date periodEnd;
    private BigDecimal regularHours = BigDecimal.ZERO;
    private BigDecimal overtimeHours = BigDecimal.ZERO;
    private BigDecimal weekendHours = BigDecimal.ZERO;
    private BigDecimal holidayHours = BigDecimal.ZERO;
    private BigDecimal deductions = BigDecimal.ZERO;
    private BigDecimal grossPay = BigDecimal.ZERO;
    private BigDecimal netPay = BigDecimal.ZERO;
    private String payoutMethod; // DIRECT_DEPOSIT, CASH, CHECK
    private String paymentReference;
    private Timestamp createdAt;

    // Itemized tax withholding (computed from gross_pay via simplified flat-rate approximation
    // of federal/NY state/NYC city withholding, plus the real fixed FICA rates for Social
    // Security and Medicare). See PayrollService for the rates used.
    private BigDecimal federalTax = BigDecimal.ZERO;
    private BigDecimal socialSecurity = BigDecimal.ZERO;
    private BigDecimal medicare = BigDecimal.ZERO;
    private BigDecimal stateTax = BigDecimal.ZERO;
    private BigDecimal cityTax = BigDecimal.ZERO;
    private Date payDate;
    private String checkNumber;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Date getPeriodStart() { return periodStart; }
    public void setPeriodStart(Date periodStart) { this.periodStart = periodStart; }

    public Date getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Date periodEnd) { this.periodEnd = periodEnd; }

    public BigDecimal getRegularHours() { return regularHours; }
    public void setRegularHours(BigDecimal regularHours) { this.regularHours = regularHours; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getWeekendHours() { return weekendHours; }
    public void setWeekendHours(BigDecimal weekendHours) { this.weekendHours = weekendHours; }

    public BigDecimal getHolidayHours() { return holidayHours; }
    public void setHolidayHours(BigDecimal holidayHours) { this.holidayHours = holidayHours; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }

    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }

    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }

    public String getPayoutMethod() { return payoutMethod; }
    public void setPayoutMethod(String payoutMethod) { this.payoutMethod = payoutMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public BigDecimal getFederalTax() { return federalTax; }
    public void setFederalTax(BigDecimal federalTax) { this.federalTax = federalTax; }

    public BigDecimal getSocialSecurity() { return socialSecurity; }
    public void setSocialSecurity(BigDecimal socialSecurity) { this.socialSecurity = socialSecurity; }

    public BigDecimal getMedicare() { return medicare; }
    public void setMedicare(BigDecimal medicare) { this.medicare = medicare; }

    public BigDecimal getStateTax() { return stateTax; }
    public void setStateTax(BigDecimal stateTax) { this.stateTax = stateTax; }

    public BigDecimal getCityTax() { return cityTax; }
    public void setCityTax(BigDecimal cityTax) { this.cityTax = cityTax; }

    public BigDecimal getTotalTaxes() {
        return federalTax.add(socialSecurity).add(medicare).add(stateTax).add(cityTax);
    }

    public Date getPayDate() { return payDate; }
    public void setPayDate(Date payDate) { this.payDate = payDate; }

    public String getCheckNumber() { return checkNumber; }
    public void setCheckNumber(String checkNumber) { this.checkNumber = checkNumber; }
}
