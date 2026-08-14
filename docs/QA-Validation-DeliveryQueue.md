# QA Validation Report — Delivery Queue

**Scope:** End-to-end validation of the Delivery Queue page (`DeliveryPanel.java`) — the
screen that tracks delivery orders through their documented lifecycle — combining static code
review (`DeliveryPanel.java`, `DeliveryDAO.java`, `POSPanel.java`'s delivery-address handling)
with live functional testing against a real order placed through checkout.

**Relationship to prior reports:** Delivery Queue is downstream of POS/Checkout — a delivery
row is created automatically (status `UNASSIGNED`) the moment an order with
`orderType = DELIVERY` is checked out (`OrderDAO`/`POSPanel` → `DeliveryDAO.createDelivery()`).
This report focuses on the Delivery Queue screen's own operations (Assign, Mark Picked Up, Mark
Delivered) and the state machine documented in `DeliveryPanel.java`'s class Javadoc:
`UNASSIGNED -> ASSIGNED -> PICKED_UP -> DELIVERED`. It does not re-validate checkout's payment
or stock-deduction logic, which were covered in the POS/Inventory reports.

## Summary

| Result | Count |
|---|---|
| Passed | 1 |
| Bugs found | 3 (2 Critical, 1 Low) |

## Passed

1. **Delivery address is required and cleanly validated at checkout.** Confirmed by code review
   and live testing: selecting order type DELIVERY in POS immediately prompts for a delivery
   address (`POSPanel.onOrderTypeChanged()`); cancelling or leaving it blank reverts the order
   type back to DINE_IN rather than allowing an invalid half-configured DELIVERY order.
   `openPaymentsScreen()` independently re-checks that `deliveryAddress` is non-null/non-empty
   before allowing payment, so there's no path to a delivery order with a missing address. Live
   checkout of a real DELIVERY order ("123 QA Test St, New York, NY", Order #189) correctly
   created a new `UNASSIGNED` row in the Delivery Queue with the address, phone, and total all
   displayed correctly.

## Bugs found

### 1. "Mark Delivered" has no precondition — an order can be marked DELIVERED while still UNASSIGNED, with no driver ever recorded — Critical

Reproduced live: Order #189 was created via real checkout and landed in the queue as
`UNASSIGNED` with a blank "Assigned To" field, exactly as designed. Selecting that row and
clicking **Mark Delivered** directly — skipping Assign to Driver and Mark Picked Up entirely —
succeeded with no warning or validation. Toggling "Show completed deliveries" confirmed the
order now shows **status DELIVERED with a completely blank Assigned To field**, contradicting
the state machine documented in the panel's own class Javadoc
(`UNASSIGNED -> ASSIGNED -> PICKED_UP -> DELIVERED`).

The root cause is a straightforward inconsistency in `DeliveryPanel.java`: the "Mark Picked Up"
button's handler explicitly guards against this (`updateStatus()` lines 114–118 reject the
PICKED_UP transition if `assignedTo == null`), but the "Mark Delivered" handler
(`deliveredBtn.addActionListener`, line 45) calls the exact same `updateStatus(d, "DELIVERED")`
with no equivalent guard, and `DeliveryDAO.updateStatus()` (lines 69–79) itself performs a bare
`UPDATE ... SET status = ?` with no validation of the current status or of legal transitions at
all. In practice this means a customer's order can be recorded as successfully delivered with no
driver ever assigned and no pickup ever confirmed — a serious gap for any business relying on
this screen to know which orders actually went out and who took them.

**Where:** `DeliveryPanel.java` line 45 (`deliveredBtn` handler, missing guard) vs. lines
113–118 (`updateStatus()`, shows the guard pattern that should also apply to DELIVERED);
`DeliveryDAO.java` lines 69–79 (`updateStatus()` performs no state-transition validation at all).
**Suggested fix:** move the transition rules into `updateStatus()` in the panel (or better, into
`DeliveryDAO` itself so it can't be bypassed by any future caller) so that DELIVERED can only be
reached from PICKED_UP, and PICKED_UP only from ASSIGNED, rejecting any skip with a clear
validation message like the existing PICKED_UP check.

### 2. "Assign to Driver..." silently un-delivers a completed order — Critical

Reproduced live immediately following Bug 1: with Order #189 now sitting at DELIVERED (via the
bug above), selecting that same row again and clicking **Assign to Driver...** opened the normal
"Driver / staff name:" input dialog defaulting to blank. Entering "QA Test Driver" and clicking
OK succeeded with no warning — and the queue immediately showed Order #189's status **flip back
from DELIVERED to ASSIGNED**, with the new driver name now populated. There is no confirmation
dialog, no indication that the order had previously been completed, and no record left behind
that it was ever DELIVERED in the first place.

This reproduces even starting from a legitimately-earned DELIVERED status (not just one reached
via Bug 1), because `DeliveryDAO.assign()` (lines 57–67) is a bare
`UPDATE deliveries SET assigned_to = ?, status = 'ASSIGNED' WHERE id = ?` with no check of the
row's current status before overwriting it. Any already-PICKED_UP or already-DELIVERED order can
be silently reset to ASSIGNED by re-running Assign to Driver on it, e.g. to reassign a driver —
a plausible everyday action — which as a side effect erases a completed delivery's
completion status with zero audit trail of the change.

**Where:** `DeliveryDAO.java` lines 57–67 (`assign()`, unconditional status overwrite);
`DeliveryPanel.java` lines 97–111 (`assignDialog()`, no current-status check before calling
`assign()`).
**Suggested fix:** either block re-assignment once a delivery has reached PICKED_UP/DELIVERED
(requiring an explicit "Unassign"/"Reopen" action instead), or at minimum have `assign()` only
update `assigned_to` without resetting `status` when the delivery is already past ASSIGNED, plus
a confirmation prompt when re-assigning a delivery that isn't UNASSIGNED.

### 3. No cancel / failed-delivery status, and `updateStatus()` accepts any string — Low

Confirmed by code review: the entire app only ever writes four status values
(`UNASSIGNED`, `ASSIGNED`, `PICKED_UP`, `DELIVERED`), and there is no UI path to mark a delivery
as cancelled, failed, or returned — an order that can't be delivered (wrong address, customer
unreachable, etc.) has no correct end state to move to. Compounding this, `DeliveryDAO.updateStatus()`
takes a raw `String status` with no validation against a known set of values, so any future caller
passing a typo'd or unexpected string would be written straight to the database with no error.

**Where:** `DeliveryDAO.java` lines 69–79 (`updateStatus()`, unchecked status string);
`DeliveryPanel.java` (no Cancel/Failed action offered anywhere in the UI).
**Suggested fix:** introduce an explicit status enum/whitelist validated in `updateStatus()`, and
add a "Cancel Delivery" action (with a required reason) so undeliverable orders have a correct
place to land instead of being stuck at their last real status or force-marked DELIVERED.

## Test data note

Order #189, used for live reproduction of Bugs 1 and 2, is left in the database in an ASSIGNED
state with "Assigned To" = "QA Test Driver" as a direct consequence of Bug 2 — there is currently
no way to fully reset it to a clean state through the UI (see Bug 3: no cancel option exists).
This is itself evidence supporting Bug 3's finding.
