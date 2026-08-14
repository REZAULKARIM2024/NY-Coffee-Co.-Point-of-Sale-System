# QA Validation Report — Reports

**Scope:** End-to-end validation of the Reports page (`ReportsPanel.java`, left sidebar) — the
"Today's Sales Summary" dashboard showing Orders Today, Total Sales, Tax Collected, Discounts
Given, and a Top Sellers Today table — combining static code review (`ReportsPanel.java`,
`ReportDAO.java`) with live functional testing, including live checkout transactions and a direct
SQL query against the MySQL server to verify the page's day-boundary logic.

**Relationship to prior reports:** This is a different screen from the "Reports" group under
Functions (validated in the Functions QA report, which covers a 30-day Menu Item Sales report
reached via POS/Checkout → Functions → Reports). This left-sidebar Reports page is a simpler,
real-time, today-only dashboard fed by `ReportDAO`, independent of `operationsDAO` used by the
Functions screen. This report also directly follows up on the Time Clock report's Critical
timezone bug (`DBConnection.java`'s `serverTimezone=UTC`) to determine whether it also affects
this page's "today" boundary — see Passed #3 below for the answer.

## Summary

| Result | Count |
|---|---|
| Passed | 4 |
| Bugs found | 2 (1 Medium, 1 Low) |

## Passed

1. **Today's Sales Summary aggregates real order data with exact, penny-accurate math.**
   Verified live across three real checkout transactions created during this session: after a
   $10.79 delivery sale (from the Delivery Queue report) and a fresh $10.79 retail sale, the page
   correctly showed Orders Today: 2, Total Sales: $21.58 ($10.79 × 2), Tax Collected: $1.60
   ($0.80 × 2), Top Sellers: Ground Coffee Bag, qty 2, $19.98 ($9.99 × 2) — all exact. A third
   order (a $1.99 cookie with a $1.00 discount, actual charge $1.07) brought the totals to Orders
   Today: 3, Total Sales: $22.65, Tax Collected: $1.68, **Discounts Given: $1.00** — every figure
   reconciled exactly against the underlying orders with no rounding or aggregation errors.
2. **Refresh and page navigation both reflect new orders immediately, with no caching issues.**
   Verified live: leaving the Reports tab and returning to it (which recreates the panel) picked
   up each new order right away, and the explicit Refresh button reproduced the same result
   on demand — `loadData()` re-queries `ReportDAO` fresh every time, with no stale in-memory
   state.
3. **The "today" boundary is computed correctly and is immune to the timezone bug found in the
   Time Clock report.** The Time Clock report identified a Critical bug where `DBConnection.java`
   line 34 hardcodes `serverTimezone=UTC` in the JDBC URL, causing timestamps *read back into
   Java* to display several hours off from real local time. This raised the question of whether
   Reports' `WHERE DATE(created_at) = CURDATE()` boundary (`ReportDAO.java` lines 18–20, 48) is
   also affected. A direct SQL query against the MySQL server
   (`SELECT NOW(), CURDATE(), @@session.time_zone, @@global.time_zone, @@system_time_zone`) showed
   `server_now = 2026-08-14 01:07:54` against a real wall-clock time of 1:07:54 AM — an exact
   match — with `system_time_zone = 'Eastern Daylight Time'` and session/global both set to
   `SYSTEM`. This confirms the MySQL server's own clock is correctly set to real local time, and
   because `getTodaySummary()`/`getTopItemsToday()` only ever fetch aggregated `SUM`/`COUNT`
   values and item names (never a raw `Timestamp` object) into Java, the JDBC `serverTimezone`
   misconfiguration never enters into this query at all. Live evidence: a delivery order placed
   at roughly 12:27 AM correctly appeared under "Today" when checked minutes into the new
   calendar day. **This narrows the scope of the previously-reported timezone bug**: it affects
   Java-side *display* of individual timestamp values (Time Clock's "Clocked IN since...",
   Delivery Queue's "Placed" column) but not server-side date-boundary aggregation like this
   report uses.
4. **The actual amount charged always reflects an entered discount correctly, even when the
   on-screen total does not (see Bug 1).** Verified live: despite the Payments screen displaying
   a stale, pre-discount Total of $2.15 for a $1.99 item with a $1.00 discount (see Bug 1), the
   completed transaction dialog correctly showed "Order #191 completed. Total: $1.07" — the
   correctly-discounted amount — and Reports' Discounts Given figure picked up the full $1.00
   afterward. The money math that actually gets charged and recorded is trustworthy; only the
   display lags behind it.

## Bugs found

### 1. Entering a discount doesn't update the visible Total unless Enter is pressed — Medium

Reproduced live: on the POS/Checkout screen, typing an amount into the "Discount $" field and
then pressing Tab (or clicking anywhere else) leaves the Subtotal/Tax/Total labels completely
unchanged — still showing the pre-discount total — both on the main cart panel and after
proceeding to the Payments screen. The stale total persists all the way up to the payment keypad,
where a cashier would naturally tender cash based on the number on screen. In this session's test,
a $1.99 item with a $1.00 discount continued to display a $2.15 total (no discount applied) right
up until CASH was pressed — at which point the backend silently corrected itself and charged the
true discounted amount ($1.07). The root cause is in `POSPanel.java` line 1255:
`discountField.addActionListener(e -> recalcTotals())` — a `JTextField`'s `ActionListener` only
fires when the user presses Enter while the field has focus; Tab-ing away or clicking elsewhere
never triggers it, and `openPaymentsScreen()` (lines 1823–1841) does not call `recalcTotals()`
either before showing the Payments screen.

While the actual charged amount turned out to be correct in this build (a separate, independent
calculation path is used at the moment of payment), the on-screen number a cashier and customer
both see can be flatly wrong right up until checkout, which is a serious trust and dispute risk
even though it happens not to cause an actual billing error today.

**Where:** `POSPanel.java` line 1255 (`discountField` only wired to `ActionListener`);
`POSPanel.java` lines 1823–1841 (`openPaymentsScreen()`, no `recalcTotals()` call).
**Suggested fix:** attach a `DocumentListener` (or a focus-lost listener) to `discountField` so
every keystroke or focus change recalculates the displayed totals, and call `recalcTotals()`
defensively at the top of `openPaymentsScreen()` so the Payments screen can never show a stale
number regardless of how the discount field was left.

### 2. Reports page only ever shows "Today," with no historical or date-range view — Low

Confirmed by code review: `ReportsPanel.java` has no date picker or navigation of any kind —
`getTodaySummary()` and `getTopItemsToday()` are both hardcoded to `CURDATE()`. There is no way
to check yesterday's numbers, a custom range, or week/month-to-date totals from this screen, and
no link to the more detailed 30-day Menu Item Sales report that exists under POS/Checkout →
Functions → Reports (see the Functions QA report). A manager wanting a Reports page - which is
the more prominent, dedicated left-sidebar destination for this - would reasonably expect it to
offer at least a basic date range, especially since the data (via `orders`/`order_items`) clearly
supports it.

**Where:** `ReportsPanel.java` (no date-selection UI); `ReportDAO.java` lines 17–35, 43–67
(hardcoded to `CURDATE()`).
**Suggested fix:** add a simple date picker (defaulting to today) and thread the chosen date
through both DAO methods in place of `CURDATE()`, and/or add a visible link over to the Functions
→ Reports screen for longer-range analysis.

## Test data note

Three real orders were created during live testing (Order #189 from the Delivery Queue report,
plus two new orders created specifically for this report: a $10.79 Ground Coffee Bag cash sale
and a $1.99 Chocolate Chip Cookie with a $1.00 discount, charged correctly at $1.07). These are
left in the database as completed, real transactions — there is no cancel/delete function
available anywhere in the app to remove them (consistent with prior reports' findings), so they
remain reflected in Today's Sales Summary going forward.
