# QA Validation Report — POS / Checkout: Sandwiches

**Scope:** End-to-end validation of the Sandwiches tab (subcategories, item customization, cart,
payment, order persistence), combining static code review (`POSPanel.java`, `CartItem.java`) with
live functional testing.

**Relationship to prior reports:** Sandwiches items flow through the exact same
customize/cart/discount/payment code as Beverages, Featured, and Bakery (`openCustomize`,
`rebuildCustomizeBody`, `confirmCustomize`, `POSService.checkout`). Every bug already logged in
[QA-Validation-Beverages-Checkout.md](QA-Validation-Beverages-Checkout.md) (negative discount,
dead quick-cash buttons, silent invalid-discount fallback, no Cancel Order confirmation) and the
generic-customize-screen / "Hot"-prefix bugs logged in
[QA-Validation-Bakery-Checkout.md](QA-Validation-Bakery-Checkout.md) apply identically here and
are not re-litigated below. This report only covers what's specific to Sandwiches.

## Summary

| Result | Count |
|---|---|
| Passed | 2 |
| Bugs found (same root cause as Bakery report, confirmed here too) | 1 |

## Passed

1. **Real subcategories load correctly.** Sandwiches is backed by real `categories` rows
   (Breakfast Sandwiches, Specialty Sandwiches, Wraps & Bowls, Sides & Bites, Snacking), and items
   render correctly grouped by section (e.g. "Classic Breakfast", "Specialty Breakfast"),
   confirmed live with real pricing.
2. **Full item → cart → checkout math is correct, including cash change calculation.** Verified
   live: Bacon Egg and Cheese on English Muffin, Large ($4.99 + $1.00) → Subtotal $5.99, Tax
   $0.48, Total $6.47. Checked out with $40.00 cash tendered → Order #184 completed, Change Due
   $33.53, correct to the cent ($40.00 − $6.47 = $33.53). No inventory/recipe issue encountered on
   this item.

## Bugs found

### 1. Every Sandwiches item's customize screen shows drink-only options — Medium (same root cause as Bakery Bug 1)

Reproduced live: "Bacon Egg and Cheese on English Muffin" opens the same customize screen as a
coffee drink, including a **Temperature: Hot / Iced** selector and **Flavor Swirl** / **Add-On**
rows (Caramel/Vanilla/Mocha Swirl, Extra Espresso Shot, Whipped Cream). This is arguably even more
jarring here than on Bakery items — the confirmed cart line literally read "**Hot** Bacon Egg and
Cheese on English Muffin," and the UI offers to add a Caramel Swirl or an espresso shot to a
breakfast sandwich. Root cause is identical to the Bakery report: `allSizes`/`allModifiers`
(`POSPanel.java` lines 65–66, 121–122) are loaded once, globally, with no per-department/per-item
filtering, and `rebuildCustomizeBody()` (lines 1611–1667) renders all of them unconditionally.
Pricing and checkout are unaffected — this is a UX/data-modeling gap, not a pricing bug — but it's
worth flagging again here since Sandwiches is the tab where it's most visible to an actual
cashier or customer.

**Where:** `POSPanel.rebuildCustomizeBody()`; same suggested fix as the Bakery report (scope
sizes/modifiers to category, or hide drink-only rows outside beverage departments).

## Note on Sandwiches-specific "Size" options

Unlike Bakery (where "Large cookie" etc. is clearly nonsensical), the Size row (Small/Medium/
Large/Extra Large with price deltas) is at least plausible-looking for some Sandwiches items
(e.g. a wrap or bowl could reasonably come in sizes), so this part of the shared customize screen
is less obviously wrong here than the Temperature/Flavor Swirl rows — worth keeping Size but
still hiding the drink-only rows per the suggested fix above.
