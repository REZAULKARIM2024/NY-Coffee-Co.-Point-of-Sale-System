# QA Validation Report — Payroll

**Scope:** End-to-end validation of the Payroll page (`PayrollPanel.java`, left sidebar) — the
payroll run history table plus Run Payroll..., Print Paystub..., and Print Paycheck... —
combining static code review (`PayrollPanel.java`, `PayrollService.java`, `PayrollDAO.java`,
`PayrollRun.java`, `PaystubDialog.java`, `PaycheckDialog.java`) with live functional testing.

## Summary

| Result | Count |
|---|---|
| Passed | 3 |
| Bugs found | 3 (1 Critical, 1 Medium, 1 Low) |

## Passed

1. **Payroll's gross/tax/net math is correct and traceable to real time clock hours.** Code
   review of `PayrollService.computeGrossPay()`/`computeTaxes()` confirms regular hours are paid
   at the employee's base rate, with overtime (>40 weekday hrs/week) at 1.5x, weekend at 1.25x,
   and holiday at 1.5x, then federal/FICA/state/city withholding is applied at fixed rates.
   Spot-checked live against Aaliyah Wilson's existing run (22.67 regular + 15.30 weekend hours
   at her $30.52/hr rate): 22.67 × $30.52 + 15.30 × $30.52 × 1.25 ≈ $1275, matching the displayed
   Gross of $1275.48.
2. **Print Paystub and Print Paycheck both correctly reflect the selected payroll run.**
   Reproduced live: running payroll for a period with zero worked hours produced a run with
   Gross/Net $0.00, and both the Paystub and Paycheck dialogs displayed $0.00, the correct pay
   period, and the correct employee — confirming both documents pull from the actual selected
   row rather than stale or default data. The Paycheck dialog is also clearly and correctly
   labeled "For internal payroll record-keeping ... not a negotiable instrument," avoiding any
   confusion about it being a real check.
3. **Run Payroll's basic input validation works.** Reproduced live: a Period End before Period
   Start, a non-numeric Deductions value, and a non-`yyyy-mm-dd` date string are all correctly
   rejected with a clear validation message before anything is saved (see Bug 2 for what happens
   to the form afterward).

## Bugs found

### 1. Running payroll twice for the same employee and period silently pays them twice — Critical

Reproduced live, and also found already present in the existing seeded data before any testing
began: **Aaliyah Wilson already had two identical payroll runs** for the exact same period
(2026-07-18 to 2026-07-24) — same 22.67 regular + 15.30 weekend hours, same $1275.48 gross, same
$999.98 net, differing only by check number and internal id. To confirm this wasn't just a seed
artifact, Run Payroll was used again live for Aaliyah Wilson with that exact same period: the app
accepted it with no warning of any kind and produced a **third** identical run — Regular: 22.67
hrs, Weekend: 15.30 hrs, Gross: $1275.48, Net: $999.98 — bringing her total for one week of
actually-worked hours to three separate $999.98 payouts. Nothing in `PayrollPanel.openRunDialog()`,
`PayrollService.runPayroll()`, or `PayrollDAO` checks whether a `payroll_runs` row already exists
for the same `employee_id` with an overlapping period before inserting a new one —
`getEntriesForPeriod()` freely re-reads and re-pays the same closed time-clock shifts every time
Run Payroll is used, with no "already paid" flag on `time_clock` rows and no period-overlap
check on `payroll_runs`. Since Payout Method can be DIRECT_DEPOSIT, this is a genuine
double/triple real-money payment risk from a single accidental double-click or a manager
re-running payroll after an interruption, not just a display glitch.

**Where:** `PayrollPanel.java` `openRunDialog()` (lines 125–188, no existing-run check before
calling `payrollService.runPayroll()`); `PayrollService.java` `runPayroll()` (lines 53–135, no
duplicate-period guard); `PayrollDAO.java` (no query to check for overlapping
`payroll_runs` rows, and no "paid" flag on `time_clock`).
**Suggested fix:** before saving, query `payroll_runs` for any existing row with the same
`employee_id` and an overlapping `[period_start, period_end]` range, and block the run (or
require explicit confirmation naming the prior run) if one is found; alternatively, mark
`time_clock` rows as "paid" once included in a run so the same shift can never be paid out twice.

### 2. A validation failure in Run Payroll discards the whole form, same as elsewhere in the app — Medium

Reproduced live: selecting an employee, then typing "not-a-date" into Period Start and pressing
OK correctly shows "Dates must be in yyyy-mm-dd format." — but clicking OK on that message closes
the entire Run Payroll dialog, and reopening it resets Employee back to the first item in the
list and Period Start/End back to the default last-7-days range. Any employee, date, deduction,
or payout method already chosen is lost, exactly matching the same single-shot `JOptionPane`
validation pattern documented in the Employees QA report.

**Where:** `PayrollPanel.java` `openRunDialog()` (lines 148–172): one
`JOptionPane.showConfirmDialog(...)` call followed by sequential validation checks that each
`return` out of the method after the dialog has already closed.
**Suggested fix:** loop and re-show the same form (with prior selections preserved) on
validation failure, only closing on a valid submit or explicit Cancel.

### 3. The printed paystub claims direct deposit funds are "sent to the bank account on file," but no bank account is ever collected anywhere in the app — Low

Confirmed by code review and live view of a Direct Deposit paystub: the footer of every
DIRECT_DEPOSIT paystub reads "Direct deposit — funds sent to the bank account on file for
[Employee Name]." However, the Employees QA report found that `bank_name`,
`bank_account_number`, and `bank_routing_number` columns exist in the database schema but are
never read, written, or exposed anywhere in the application — there is no screen anywhere that
lets a manager or employee enter or view a bank account. The paystub is making a factual claim
about a "bank account on file" that the app has no means of ever actually having on file.

**Where:** `PaystubDialog.java` (direct deposit footer text); cross-referenced against
`Employee.java`/`EmployeeDAO.java` (no bank fields) as documented in
`docs/QA-Validation-Employees.md`.
**Suggested fix:** either implement the bank-account fields on the Employee record so the claim
is true, or soften the paystub wording (e.g. "Direct deposit — see your enrollment records for
account details") until that data actually exists in the system.

## Cross-reference note

Bug 1 has a second-order effect worth flagging: `PayrollDAO.getYtdTotals()` sums every
`payroll_runs` row for the employee/year with no de-duplication, so the duplicate/triple runs
created above are not just extra table rows — they directly inflate the Year-to-Date gross,
tax, and net figures shown on that employee's paystub going forward, which would be a real
problem for W-2 or year-end tax reporting if this build were used for actual payroll.

## Test data note

Three payroll runs were created during live testing, all for Aaliyah Wilson: one legitimate
$0.00 run for a period with no worked hours (2026-08-08 to 2026-08-14, check #1000408, used to
verify the Paystub/Paycheck dialogs), and one additional duplicate of the pre-existing
2026-07-18–2026-07-24 run (check #1000409) created specifically to reproduce Bug 1 live —
bringing that employee's total recorded runs for that single period to three. There is no delete
function for payroll runs anywhere in the app, so all of these remain in the database, consistent
with the no-delete pattern noted in prior QA reports.
