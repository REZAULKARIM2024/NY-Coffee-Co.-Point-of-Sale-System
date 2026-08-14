# QA Validation Report — Employees

**Scope:** End-to-end validation of the Employees page (`EmployeePanel.java`, left sidebar) —
the employee directory table plus Add Employee, Edit Selected, and Create Login for Selected —
combining static code review (`EmployeePanel.java`, `EmployeeDAO.java`, `UserDAO.java`,
`Employee.java`, `database/schema.sql`) with live functional testing.

## Summary

| Result | Count |
|---|---|
| Passed | 3 |
| Bugs found | 4 (2 Medium, 2 Low) |

## Passed

1. **Add Employee correctly validates and persists new employees.** Reproduced live: a blank
   name is rejected ("Name is required."), an invalid phone like "abc" is rejected ("Enter a
   valid phone number or leave it blank."), and a fully valid entry ("QA Test Employee", QA
   Tester, (212) 555-0199, qa.test@nycoffee.example, $18.50/hr) saved correctly and immediately
   reappeared in the table, correctly re-sorted alphabetically by name.
2. **Edit Selected correctly loads existing values and persists changes.** Reproduced live:
   opening Edit on the newly-added test employee pre-filled every field exactly as saved;
   changing Hourly Rate from $18.50 to $20.00 and unchecking Active saved correctly and the
   table immediately reflected both changes ($20.00, Active: false).
3. **Create Login for Selected successfully creates a working, employee-linked user account,**
   with real validation. Reproduced live: leaving the password blank is rejected ("Username is
   required and password must be at least 6 characters."); a valid username/password/role
   combination creates the account and links it via `employee_id` to the selected employee, and
   is immediately confirmed with "Login created for QA Test Employee."

## Bugs found

### 1. A validation failure in Add/Edit Employee silently discards everything the user typed — Medium

Reproduced live: filling in Full Name ("QA Test Employee"), Position ("QA Tester"), and an
invalid Phone ("abc"), then clicking OK, correctly shows "Enter a valid phone number or leave it
blank." — but clicking OK on *that* message closes the entire Add Employee dialog, not just the
error. Re-opening Add Employee shows a completely blank form; the Full Name and Position that
were already typed are gone and must be re-entered from scratch. This happens because
`openEditor()` calls `JOptionPane.showConfirmDialog()` once and only validates the fields
*after* that dialog has already returned and closed — there is no re-prompt loop that keeps the
form open with the invalid field highlighted.

**Where:** `EmployeePanel.java`, `openEditor()` (lines 89–115): a single
`JOptionPane.showConfirmDialog(...)` call followed by sequential validation checks, each of
which `return`s out of the method (and out of the now-closed dialog) on failure.
**Suggested fix:** wrap the validation in a loop that re-shows the same form (with the user's
already-typed values still in the fields) whenever validation fails, only closing the dialog on
a fully valid submission or an explicit Cancel — the same fix pattern noted for other
single-shot `JOptionPane` forms elsewhere in this app.

### 2. Duplicate-username login creation shows a raw SQL error to the end user — Low

Reproduced live: selecting an employee, clicking "Create Login for Selected", and entering the
already-taken username "admin" with a valid password produces the dialog: **"Failed to create
login: Failed to create user: Duplicate entry 'admin' for key 'users.username'"** — the raw
JDBC/MySQL exception text, not a friendly message. A cashier or manager creating logins for new
hires would see internal database terminology instead of a clear "That username is already
taken, please choose another" prompt.

**Where:** `EmployeePanel.java`, `createLogin()` (lines 156–161), catches `RuntimeException` and
displays `ex.getMessage()` directly; `UserDAO.createUser()` (lines 40–56) relies entirely on the
database's `UNIQUE` constraint on `users.username` (`schema.sql` line 10) with no pre-check.
**Suggested fix:** catch the specific duplicate-key case (or pre-check with a `SELECT` before
inserting) and show a plain-language message instead of the raw SQL exception text.

### 3. An employee can silently end up with multiple, disconnected login accounts — Medium

Reproduced live: after successfully creating a login for "QA Test Employee" (username
`qa.test.employee`), selecting the same employee and clicking "Create Login for Selected" again
with a different username (`qa.test.employee2`) succeeded with no warning — the employee now has
two separate, fully independent user accounts, both linked to the same `employee_id`. The
Employees page has no way to see whether an employee already has a login, view their existing
username, reset their password, or deactivate just their login (only the employee record itself
can be marked inactive, which does not disable any of their login accounts).

**Where:** `EmployeePanel.java`, `createLogin()` (lines 133–162) — no query against `UserDAO`
to check for an existing account before showing the Create Login form; `UserDAO.createUser()`
has no `employee_id` uniqueness check either.
**Suggested fix:** before opening the Create Login form, query for any existing user rows with
the employee's `employee_id`, and if found, show their username (offer a password reset or role
change) instead of a blank "create new login" form.

### 4. The employee payment-method fields that exist in the database are never used anywhere in the app — Low

Confirmed by code review: `database/schema.sql` (lines 21–34) defines `payment_preference`
(`DIRECT_DEPOSIT`/`CASH`/`CHECK`), `bank_name`, `bank_account_number`, and
`bank_routing_number` columns on the `employees` table — but a full search of the codebase shows
none of these columns are ever read or written: they are absent from the `Employee` model,
absent from `EmployeeDAO.mapRow()`/`bind()`, and there are no matching fields anywhere in the Add
Employee / Edit Employee form. Payroll's paystub/print-paycheck feature (validated in the
Payroll area previously) does not draw on this data either. The schema promises a payment-method
and direct-deposit workflow that the application never actually delivers or exposes.

**Where:** `database/schema.sql` lines 21–34 (columns exist); `Employee.java` (no matching
fields); `EmployeeDAO.java` `mapRow()`/`bind()` (columns never selected or bound);
`EmployeePanel.java` `openEditor()` (no corresponding form fields).
**Suggested fix:** either add Payment Preference / Bank fields to the Add/Edit Employee form and
wire them through the DAO, or remove the unused columns from the schema so it doesn't overstate
what the app currently does.

## Cross-reference note

The Role dropdown (CASHIER/MANAGER/ADMIN) shown in "Create Login for Selected" is not purely
decorative — `RecipePanel.java` and `TimeClockPanel.java` do call `currentUser.isManagerOrAbove()`
to gate certain actions, so Role has real effect there. However, the Functions QA report found
that `POSPanel.java`'s Functions tab never checks role at all, so an account created here with
Role "CASHIER" still gets full access to Functions' sensitive manager-only actions (price
overrides, refunds, Close Application, etc.) — the same Critical gap documented previously,
simply confirmed again from the account-creation side.

## Test data note

One test employee ("QA Test Employee", id 103) and two test login accounts
(`qa.test.employee`, `qa.test.employee2`) were created during live testing. The employee record
was set back to Active: false to keep it out of active use, and its Hourly Rate was left at
$20.00 from the Edit test. There is no delete function for employees or user accounts anywhere
in the app, so all three records remain in the database, consistent with the no-delete pattern
noted in prior QA reports.
