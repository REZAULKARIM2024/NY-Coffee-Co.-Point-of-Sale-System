# QA Validation Report — Time Clock

**Scope:** End-to-end validation of the Time Clock page (`TimeClockPanel.java`) — the
self-service clock in/out screen and its manager-facing "all employees" punch feed —
combining static code review (`TimeClockPanel.java`, `PayrollDAO.java`,
`PayrollService.java`, `DBConnection.java`) with live functional testing.

**Relationship to prior reports:** Time Clock is upstream of Payroll — every `time_clock` row
created here is the raw input `PayrollService.runPayroll()` later turns into paid hours (regular/
overtime/weekend/holiday buckets). This report focuses on Time Clock's own two operations
(Clock In, Clock Out) and the manager overview; it does not re-validate Payroll's pay-calculation
math itself.

## Summary

| Result | Count |
|---|---|
| Passed | 4 |
| Bugs found | 3 (1 Critical, 1 High, 1 Low/Medium) |

## Passed

1. **Clock In / Clock Out works correctly end-to-end.** Verified live: clicking Clock In
   immediately flipped the status label to "Clocked IN since ...", changed the button to a red
   "Clock Out", and added a new row to the "Recent punches" table showing "(still clocked in)".
   Clicking Clock Out one minute later correctly closed that same row with a clock-out time and
   reverted the status/button back to "Currently clocked OUT." / "Clock In".
2. **Double clock-in/out is properly guarded.** `PayrollDAO.clockIn()` and `clockOut()` each
   check `getOpenEntry()` first and throw a clear error ("Already clocked in." / "Not currently
   clocked in.") if the action doesn't make sense given the current state — and since the UI only
   ever shows one button (Clock In *or* Clock Out, never both), a user can't trigger this through
   normal use; it's a solid defense-in-depth check.
3. **Payroll safely ignores incomplete or zero-length shifts rather than miscalculating.**
   Confirmed by code review: `PayrollService.runPayroll()` explicitly skips any entry where
   `clockOut` is still null and any entry where the computed duration is zero or negative, so a
   currently-open shift or an instant in/out doesn't corrupt a payroll run's totals.
4. **Employees without a linked login are cleanly blocked**, not crashed: if
   `currentUser.getEmployeeId()` is null, the panel disables the Clock In button and shows "This
   login isn't linked to an employee record — ask an admin to link one." instead of throwing an
   exception.

## Bugs found

### 1. Clock-in/out timestamps are recorded several hours off from actual local time — Critical

Reproduced live: clocking in at the real local time of roughly 1:24 AM (per the Windows clock)
was recorded and displayed by the app as **"Clocked IN since 08/12/2026 9:24 PM"** — about 4
hours earlier and technically the previous calendar day. Clocking out a minute later showed the
same ~4 hour offset (displayed 9:25 PM against a real time of ~1:25 AM), confirming this is a
consistent, systematic shift rather than a one-off glitch. The root cause is in
`DBConnection.java` line 34: the JDBC URL hardcodes `serverTimezone=UTC`, telling the MySQL
driver to interpret every timestamp coming from `NOW()` as UTC — but the app is clearly built to
run on New York local time (the "Brewed in the Heart of NYC" branding, and
`PayrollService`'s NY State/NYC city tax rates), so if the underlying MySQL server session isn't
actually configured for UTC, every stored and displayed timestamp is shifted by the difference
between UTC and Eastern time.

This isn't just cosmetic: `PayrollService.runPayroll()` (lines 70–81) classifies every shift's
pay bucket — weekday vs. weekend vs. holiday, and which ISO week its hours count against for the
40-hour overtime threshold — directly from this same stored `clock_in` timestamp
(`shiftDate.getDayOfWeek()`, `payrollDAO.isHoliday(...)`). A shift that actually started, say,
11:30 PM Friday night could be stored as if it started ~7:30 PM the same evening (still Friday,
no visible problem) or, depending on the exact offset and time of day, could shift a late-night
punch across a calendar day boundary entirely — silently changing whether those hours are paid
at the weekend premium, count toward Friday's or Saturday's totals, or land in the correct
overtime week.

**Where:** `DBConnection.java` line 34 (`serverTimezone=UTC` in the JDBC URL);
`PayrollService.java` lines 70–81 (date/weekday classification consumes the resulting timestamp
as-is).
**Suggested fix:** set `serverTimezone` to the actual timezone the MySQL server is running in (or
explicitly to `America/New_York` if that's the intended business timezone), verify with a fresh
clock-in that the displayed time matches the real wall clock, and audit any already-stored
`time_clock`/`orders`/etc. timestamps for the same offset.

### 2. No way anywhere in the app to fix a forgotten clock-out — High

Confirmed by code review: `TimeClockPanel.java` is the only GUI file in the entire codebase that
touches `TimeClockEntry` (verified via project-wide search), and it exposes exactly two actions —
Clock In and Clock Out — both of which always stamp the current moment via `NOW()`
(`PayrollDAO.java` lines 35 and 50). There is no update or delete method for `time_clock` rows
anywhere in `PayrollDAO`, and neither the Employees page nor the Payroll page offers any manual
time-entry editor. Two consequences follow directly from this: (1) if an employee forgets to
clock out, `PayrollService.runPayroll()` (line 62) silently drops that entire shift from payroll
— the class-level Javadoc even documents this as "Only closed shifts ... are counted" — so those
hours are simply never paid, and (2) the *only* way to close a forgotten entry is to press Clock
Out whenever someone finally notices, which stamps whatever the current moment is and can produce
a wildly wrong multi-hour (or multi-day) shift duration with no way to correct it afterward
either.

**Where:** `TimeClockPanel.java` (only Clock In/Out, no edit path); `PayrollDAO.java` (no
update/delete for `time_clock`); `PayrollService.java` line 62 (silently drops open shifts).
**Suggested fix:** add a manager-only "Edit Punch" capability (adjust clock-in/out time or
manually close a stuck entry with a specified time) alongside an audit trail of who changed what,
similar in spirit to Inventory's adjustment-reason logging.

### 3. The "Recent punches — all employees" panel isn't actually live — Low/Medium

`TimeClockPanel.java`'s own class Javadoc (line 16) describes this panel as showing "a live feed
of everyone's recent punches," but confirmed by code review: `loadAllEntries()` is only ever
called from the constructor and from the current user's own `toggleClock()` — there is no polling
timer and no manual refresh button. A manager who opens this tab and leaves it open will not see
a different employee's clock-in appear on screen until the manager personally clocks in or out
(or reopens the page), which doesn't match the "live feed" description.

**Where:** `TimeClockPanel.java`, `loadAllEntries()` (lines 121–136) and its two call sites (line
57, constructor; line 115, `toggleClock()`).
**Suggested fix:** add a small manual Refresh button next to the punches table, or a lightweight
periodic timer, so the manager view reflects other employees' activity without requiring the
manager's own clock action.
