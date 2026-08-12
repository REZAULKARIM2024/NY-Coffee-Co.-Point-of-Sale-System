# QA Validation Report — POS / Checkout: Beverages

**Scope:** End-to-end validation of the Beverages tab checkout flow (item selection →
customization → cart → discount → payment → order persistence), combining static code review
(`POSPanel.java`, `POSService.java`, `CartItem.java`) with live functional testing against a real
MySQL-backed instance.

**Method:** Boundary/edge-case test design against the actual pricing and validation logic (not
guessed), each result independently verified against the source math before being logged as pass
or fail.

## Summary

| Result | Count |
|---|---|
| Passed | 8 |
| Bugs found | 5 |
| Data issue found (not a code bug) | 1 |

## Passed

1. **Size/temperature/modifier pricing** — unit price correctly sums base price + size delta +
   every selected modifier delta (verified: Cold Brew $3.75 + Large $1.00 + Caramel Swirl $0.50 +
   Whipped Cream $0.50 = $5.75 exactly, matching the cart line and `CartItem.getUnitPrice()`).
2. **Multi-item subtotal** — summing two customized items produced the correct subtotal
   ($5.75 + $2.75 = $8.50).
3. **Tax calculation** — 8% tax, HALF_UP rounding, correct on every case tried (matches
   `POSServiceTest`'s existing coverage; live UI figures matched to the cent).
4. **Empty-cart checkout guard** — clicking PAY with no items shows "Nothing to checkout / Cart is
   empty" and does not open the payment screen.
5. **Cash-amount-required guard** — clicking CASH before entering an amount on the keypad shows
   "Enter the cash amount the customer handed over on the keypad first" and blocks the payment.
6. **Successful checkout, end to end** — completed a real order (Original Coffee, $2.97 total,
   $5.00 tendered): order number assigned, payment reference generated
   (`CASH-SIM-<timestamp>-<rand>`), change due calculated correctly ($2.03), cart cleared
   afterward.
7. **Inventory enforcement is atomic** — attempting to check out an item whose recipe exceeds
   available ingredient stock fails cleanly with a clear "Insufficient stock" message, and the
   cart is left intact (not partially processed) so the cashier can remove the item and retry.
8. **Non-numeric discount input doesn't crash** — typing "abc" into the Discount field is silently
   treated as $0 rather than throwing an exception (see Bug 4 below for the UX gap this creates).

## Bugs found

### 1. Negative discount increases the total instead of being rejected — High

Typing a negative number (e.g. `-5.00`) into the Discount field is *subtracted* as a negative,
which mathematically *adds* it to the subtotal before tax. Reproduced: cart total $9.18 → typing
discount `-5.00` → total became **$14.58**. There is no minimum-value (≥ 0) validation on
`discountField` / `parseDiscount()` in `POSPanel.java`, and the same unclamped value is what would
be sent to `POSService.checkout()`. A cashier fat-fingering a minus sign would overcharge a
customer with no warning.

**Where:** `POSPanel.parseDiscount()` — parses to `BigDecimal` with no sign check.
**Suggested fix:** reject or clamp negative values, e.g. `return v.signum() < 0 ? BigDecimal.ZERO : v;`

### 2. Quick-cash buttons ($5 / $10 / $20) are non-functional — Medium

The three quick-amount buttons under "Remove Last / Cancel Order" render correctly but do nothing
when clicked — confirmed live (cart/totals unchanged, no dialog). Code review confirms why: in
`buildTotalsAndActions()`, the buttons are created and added to `cashRow` but never get an
`addActionListener`. They look interactive but are dead UI.

**Where:** `POSPanel.buildTotalsAndActions()`, the `for (String amt : new String[]{"$5","$10","$20"})` loop.

### 3. No visual "selected" state for Hot/Iced — Medium

Size and modifier buttons highlight green when selected (`highlightSelectedSize()`,
`refreshModifierHighlight()`), but the Temperature buttons have no equivalent — both Hot and Iced
always render the same red, so there's no way to tell by looking at the screen which one is
currently chosen. Confirmed both by code (no highlight call in `rebuildCustomizeBody()`'s temp
row) and visually (screenshots of two different items show identical button coloring regardless of
`pendingTemp`).

**Where:** `POSPanel.rebuildCustomizeBody()`, the temperature row.

### 4. Invalid discount input fails silently — Low

When the Discount field contains non-numeric text, it's treated as $0 with no error message or
visual indication — the cashier has no way to know their input was ignored versus successfully
applied as zero.

**Where:** `POSPanel.parseDiscount()`'s catch block.

### 5. "Cancel Order" has no confirmation — Low / consistency

Clicking Cancel Order immediately clears the entire cart with no "Are you sure?" prompt, unlike
comparable destructive actions elsewhere in the same codebase (e.g. "Cancel Saved/Stored Order" in
the Functions tab does confirm). A misclick loses the whole in-progress order.

**Where:** `POSPanel.buildTotalsAndActions()`, `cancelBtn` listener.

## Data issue (not a POS logic bug)

**"Cold Brew" checkout blocked by an implausible ingredient requirement.** Attempting to check out
a single Small Cold Brew failed with `Insufficient stock for item: Cinnamon Powder (need 182.00,
have 92.00)`. Cinnamon Powder is not an ingredient a Cold Brew would typically call for, and 182
units for one drink is out of proportion to any other recipe in the system. This is a recipe/seed
data problem (likely a copy-paste or unit-of-measure error when the recipe was entered), not a bug
in the checkout code — the checkout logic correctly caught it and refused to oversell inventory,
exactly as designed. Worth auditing the `recipes`/`recipe_ingredients` data for Cold Brew (and
spot-checking sibling items) before this is demoed live.

## Design note (not a defect)

Each customized item is always added as its own cart line with quantity 1 — ordering the same
drink twice (even identically customized) produces two separate lines rather than merging into one
line with quantity 2. This matches a real receipt-style POS convention (each line can be voided
independently) and is treated here as an intentional design choice, not a bug.
