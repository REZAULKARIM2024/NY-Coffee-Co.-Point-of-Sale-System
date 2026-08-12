# QA Validation Report — POS / Checkout: Functions

**Scope:** End-to-end validation of the Functions tab (Manager function board — a distinct
"client-side, not purchasable" tab, unlike every other department covered so far), combining
static code review (`POSPanel.java`, `LoginFrame.java`) with live functional testing.

**Relationship to prior reports:** Functions is architecturally different from Beverages/
Featured/Bakery/Sandwiches/Retail/Local — it has no items, no cart-based checkout, and no
Payments screen of its own. Instead it's a grid of ~40 real, DB-backed manager/cashier actions
(till & safe management, reports, employee functions, check adjustments, order queues, device
functions, and support) reached via a sidebar with 9 groups: Manager, Daily Shift Functions,
Reports, All Open Checks, Phone Orders, DT Orders, OTG Orders, Order Confirm Board, SUPPORT.
This report evaluates whether those actions work correctly and whether they're appropriately
protected — which turned out to be the central issue.

## Summary

| Result | Count |
|---|---|
| Passed | 2 |
| Bugs found | 1 (Critical) |

## Passed

1. **Reports pull real, correct data.** Verified live: Reports → Menu Item Sales (30 days, by
   units) returned a populated, correctly-sorted table spanning every department tested in prior
   reports — e.g. "12-Cup Drip Brewer" (Coffee & Tea, 14 sold, $979.86), "Charity Donation $2"
   (Charity, Deposits & Pup Cups, 6 sold, $12.00) — confirming `operationsDAO` reports aggregate
   real order data rather than showing placeholder numbers.
2. **Menu Item Price Override correctly updates the live price and confirms it.** Verified live:
   overriding "Original Coffee" (was $2.75) to $0.01 immediately updated the price, confirmed by
   a "price updated to $0.01" message and by re-opening the same function, which now showed "was
   $0.01". Reverted to $2.75 afterward to leave data clean; the second update was confirmed the
   same way. The underlying mechanism is functionally correct — the problem is who can reach it
   (see Bug 1).

## Bugs found

### 1. Every Functions/Manager action is available to every logged-in user — no role check or manager PIN anywhere — Critical

Reproduced live and confirmed by code review: logging in as a normal user gives full,
unrestricted access to the entire Functions tab, including highly sensitive actions that any
real POS system gates behind a manager override or PIN:

- **Menu Item Price Override** — change any menu item's price permanently, with only a plain
  text-input dialog (no PIN, no second approval). Reproduced live (see Passed #2 above): changed
  "Original Coffee" from $2.75 to $0.01 with three clicks and no obstacle.
- **Transaction Return** and **Manual Credit Entry** — issue a refund or credit against any order
  for any amount, with no approval step.
- **Cancel Saved/Stored Order** — cancel any open order in the system.
- **Close Application** — calls `System.exit(0)` directly, immediately killing the POS terminal
  (and any in-progress order) with only a plain Yes/No confirmation, no authorization.
- **Safe/Till management** (Open Safe, Add Funds, Close Safe, Deposit Cash, Count Till) and
  **Drawer Functions** (Paid In, Paid Out, Cash Pull, No Sale) — full cash-handling controls with
  no separation between "cashier" and "manager" duties.
- **Restart POS Terminal** — relaunches the whole application.

Code review confirms why: `POSPanel.java`'s `FUNCTIONS_SIDEBAR_ORDER` array (line 343) is shown
unconditionally to every signed-in user with no visibility check, and none of the ~40 action
handlers in `showFunctionsGroup()` (lines 610–1150) ever branch on role. The only place
`currentUser.getRole()` appears anywhere in `POSPanel.java` is line 622, where it's interpolated
into a display-only "Launch PMC" info string — it is never used in a conditional. `LoginFrame.java`
has no role-based logic either (confirmed via search — zero matches for "role"). In other words,
the `role` field exists on the `User`/`Employee` model but nothing in the checkout UI ever reads
it to restrict access.

In a real deployment this means any employee who can log in at all — not just managers — can
override prices, issue refunds/credits, cancel orders, pull cash from the drawer, and shut down
the terminal, with zero audit gate beyond whatever `employeeId` gets recorded alongside the
action. This is a materially more serious class of bug than anything found in the item-selling
departments (Beverages through Local): those bugs affect what a single order rings up for;
this one affects who can rewrite prices, refund money, or take the register offline.

**Where:** `POSPanel.java` — `FUNCTIONS_SIDEBAR_ORDER` (line 343), `selectFunctionsTab()` (line
347), and every action handler inside `showFunctionsGroup()` (lines 610–1150, e.g. the
`"Menu Item Price Override"` handler at lines 726–733 and the `"Close Application"` handler at
lines 624–627). Also `LoginFrame.java` (no role check at sign-in).
**Suggested fix:** add a role check before rendering the Functions tab (or specific sidebar
groups within it) so only Manager/Admin-tier accounts see Daily Shift Functions, Check Functions
price/return/credit actions, and Close/Restart Application; for cashier-tier accounts, either
hide those buttons or require a manager PIN prompt (a second employee's credentials) before the
action executes, matching how real POS systems separate cashier and manager duties.
