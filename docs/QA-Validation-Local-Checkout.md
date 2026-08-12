# QA Validation Report — POS / Checkout: Local

**Scope:** End-to-end validation of the Local tab (subcategories, item customization, cart,
tax, payment), combining static code review (`POSPanel.java`, `POSService.java`) with live
functional testing.

**Relationship to prior reports:** Local Items and Catering Menu products flow through the
same customize/cart/payment code as Beverages/Bakery/Sandwiches/Retail, so the generic
drink-style customize screen and "Hot"/"Iced" name-prefix bugs already logged in
[QA-Validation-Bakery-Checkout.md](QA-Validation-Bakery-Checkout.md) apply here too and aren't
re-litigated below. This report focuses on what's new and specific to Local: the **"Charity,
Deposits & Pup Cups"** subcategory, which sells charity donations and reusable-bag deposits as
ordinary menu items — a combination that turned out to produce the most direct "the receipt
doesn't match the button the customer tapped" bug found across all tabs so far.

## Summary

| Result | Count |
|---|---|
| Passed | 1 |
| Bugs found | 2 (1 Critical — shared root cause with prior reports, worse impact here; 1 Medium/High — new) |

## Passed

1. **Subcategories load correctly and checkout math is correct end to end.** All three real
   `categories` rows (Local Items, Catering Menu, Charity, Deposits & Pup Cups) render with
   their real items and pricing. Verified live: cart with Reusable Bag Deposit $.05 (Small, no
   modifiers, $0.05) + Charity Donation $5 (Small, no modifiers, $5.00) → Subtotal $5.05, Tax
   $0.40, Total $5.45 → $8.00 cash tendered → Order #186 completed, Change Due $2.55, correct to
   the cent ($8.00 − $5.45 = $2.55).

## Bugs found

### 1. The generic drink customize screen can silently change a stated donation amount — Critical (same root cause as Retail Bug 3, worse impact here)

Reproduced live: tapping "Charity Donation $5" opens the same Size/Temperature/Dairy-Sweetener/
Flavor-Swirl/Add-On customize screen as a coffee drink. Selecting "Large (+$1.00)" and
confirming produced a cart line of "**1 Hot Charity Donation $5 / Large / $6.00**" — a button
labeled and priced as a $5 donation that actually rings up at $6.00 before tax ($6.48 after 8%
tax). This is the same underlying defect already flagged on Retail gift cards, but arguably
worse here: a "$5 Charity Donation" button reads as a fixed, unmodifiable amount even more
strongly than a gift card denomination does, so a cashier or a self-serve kiosk user has even
less reason to expect — or notice — that tapping "Large" changed what the customer is actually
being charged. The same risk applies to every Catering Menu tray/platter/box, where "Large" is
at least a plausible-sounding option that could confuse a catering order's real price.

**Where:** Same root cause as prior reports — `allSizes`/`allModifiers` loaded globally with no
per-item/category filtering (`POSPanel.java` lines 65–66, 121–122, `rebuildCustomizeBody()`
lines 1611–1667).
**Suggested fix:** at minimum, skip the customize screen entirely for fixed-price items like
donations and deposits (they should ring up at their exact stated amount with no size/temp/
modifiers at all) — same general fix already suggested in the Bakery and Retail reports, applied
here to the highest-stakes case yet since it touches a monetary donation rather than a product.

### 2. Sales tax is applied to charity donations and refundable bag deposits — Medium/High

Reproduced live: even with default Small/no modifiers (no interaction with Bug 1 above), adding
a $5 Charity Donation and a $.05 Reusable Bag Deposit to the cart produced Subtotal $5.05, **Tax
$0.40**, Total $5.45 — the flat 8% `POSService.TAX_RATE` (`POSService.java` line 15) is applied
uniformly to every line in the cart's subtotal (`calculateTax()`, lines 28–30) with no exemption
logic for non-merchandise line items. In normal retail/nonprofit accounting practice, a
charitable donation is not a taxable sale, and a refundable bag deposit is not sale revenue
either, so both should typically be tax-exempt. A "Tax Exempt" button does exist on the Payments
screen (bottom-left, next to "Cancel Saved/Stored Order"), so a cashier *can* manually zero out
tax for a specific order — but it's an all-or-nothing manual toggle for the whole order, not an
automatic per-item exemption, and nothing in the UI prompts or warns a cashier to use it when a
donation or deposit item is in the cart. In practice this means every donation and bag-deposit
sale rung up the normal way (i.e. without a cashier remembering to hit Tax Exempt) overcharges
the customer and would misstate taxable sales figures on any tax report pulled from this data.

**Where:** `POSService.calculateTax()` (`POSService.java` lines 28–30) — no per-line-item
tax-category check exists anywhere in the checkout path.
**Suggested fix:** add a `taxable` flag to `MenuItem`/`CartItem` (defaulting to true for normal
merchandise, false for donation/deposit-type items), and have `calculateTax()` sum only the
taxable portion of the cart instead of the full subtotal.
