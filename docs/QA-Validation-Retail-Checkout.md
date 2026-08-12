# QA Validation Report — POS / Checkout: Retail

**Scope:** End-to-end validation of the Retail tab (subcategories, item customization, cart,
payment — including the GC Redeem payment path, since Retail is the department that actually
sells gift cards), combining static code review (`POSPanel.java`) with live functional testing.

**Relationship to prior reports:** Standard merchandise items flow through the same
customize/cart/payment code as Beverages/Bakery/Sandwiches, so the previously logged bugs
(negative discount, dead quick-cash buttons, generic drink-style customize screen, "Hot"/"Iced"
name prefix) apply here too and aren't re-litigated below. This report focuses on what's new and
specific to Retail: **gift card purchase and redemption**, which turned out to hide the most
serious bugs found across all tabs so far.

## Summary

| Result | Count |
|---|---|
| Passed | 1 |
| Bugs found | 3 (1 Critical, 1 High, 1 Medium — shared root cause with prior reports, worse impact here) |

## Passed

1. **Standard merchandise checkout works correctly end to end.** Verified live: Logo T-Shirt,
   Small, no modifiers → Subtotal $14.99, Tax $1.20, Total $16.19 → $90.00 cash tendered → Order
   #185 completed, Change Due $73.81, correct to the cent. Real subcategories (Coffee & Tea, Mugs
   & Drinkware, Gift Sets & Holiday, Grab & Go Treats, Gift Cards & More) all load correctly.

## Bugs found

### 1. "GC Redeem" never completes the sale — it's a dead end — Critical

Reproduced live: after adding an item, pressing PAY, then **GC Redeem**, entering any gift card
number (even an obviously fake one — tested `0000000000`), the app shows "Gift card ending 0000
applied." and returns to the same Payments screen. **The cart is not cleared, no order number is
generated, nothing is written to the database, and the Total is still shown unpaid.** Code review
confirms why: the `gcBtn` listener (`POSPanel.java`, lines 2073–2081) only shows an input dialog
and a confirmation message — it never calls `performCheckout()` or any equivalent of what the
CASH/Credit Card buttons do. A cashier who chooses "GC Redeem" as the payment method has no way to
actually finish that sale from this button; they would have to notice the cart never cleared and
switch to a different payment method, but nothing in the UI tells them the "applied" message did
not finish the transaction.

**Where:** `POSPanel.java`, the `gcBtn.addActionListener` block (~lines 2069–2082).
**Suggested fix:** after a successful redemption, call `performCheckout("GC")` (or equivalent)
the same way `cashBtn`/`creditBtn` do, so the order is actually persisted and the cart clears.

### 2. Gift cards sold in Retail have no real, redeemable balance anywhere in the system — High

There is no `GiftCard` model, DAO, or `gift_card`-type table anywhere in the codebase (confirmed
via full-project search — zero matches for `GiftCard`/`gift_card`). This means the "$10 Gift
Card," "$25 Gift Card," "$50 Gift Card," etc. items under Retail → Gift Cards & More are just
ordinary `MenuItem` rows: buying one adds a normal cart line and rings up like any other product,
but **no code anywhere creates an actual redeemable balance tied to a card number.** Combined with
Bug 1 above (GC Redeem doesn't validate against anything real either — it accepts literally any
non-empty string), the entire gift-card lifecycle is disconnected end to end: selling a card
creates nothing redeemable, and "redeeming" a card checks nothing real. If this were used for an
actual business, every gift card sold would be unredeemable and every "GC Redeem" payment would be
accepted for a card that was never purchased.

**Where:** No such file exists; this is an absence, not a specific line. Confirmed by grepping
`src/main/java` for `GiftCard`/`gift_card` (zero results) and reading the `gcBtn` listener in
`POSPanel.java`.
**Suggested fix:** add a `gift_cards` table (card number/code, original balance, remaining
balance, issued date, status), populate it when a gift-card `MenuItem` is checked out, and have
GC Redeem actually look up the entered code, validate remaining balance ≥ order total (or support
partial/split redemption), and deduct on success.

### 3. Gift cards go through the same generic customize screen as drinks — sizing can silently change a card's face value — Medium/High

Reproduced live: tapping "$10 Gift Card" opens the same Size/Temperature/Flavor Swirl/Add-On
customize screen as a coffee drink. Selecting "Large (+$1.00)" and confirming produced a cart line
of "**1 Hot $10 Gift Card / Large / $11.00**" — a card labeled $10 that actually rings up (and,
were Bug 2 fixed, would presumably be issued) at $11.00. Unlike the same underlying bug on
Bakery/Sandwiches items (which is a UX nuisance), this one directly risks a real monetary/face-
value mismatch on a financial instrument — a customer could be charged more or less than the
card's stated denomination with no warning, and a printed receipt showing "Hot ... Large" on a
gift card would confuse both cashier and customer.

**Where:** Same root cause as prior reports — `allSizes`/`allModifiers` are loaded globally with
no per-item/category filtering (`POSPanel.java` lines 65–66, 121–122, `rebuildCustomizeBody()`
lines 1611–1667).
**Suggested fix:** at minimum, skip the customize screen entirely for gift-card items (they should
ring up at their fixed face value with no size/temp/modifiers at all) — this is a stronger case
for the general "scope customize options per category" fix already suggested in the Bakery report.
