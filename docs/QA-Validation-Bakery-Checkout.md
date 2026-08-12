# QA Validation Report — POS / Checkout: Bakery

**Scope:** End-to-end validation of the Bakery tab (subcategories, item customization, cart,
discount, payment, order persistence), combining static code review (`POSPanel.java`,
`CartItem.java`, `OrderDAO.java`) with live functional testing.

**Relationship to prior reports:** Bakery items flow through the exact same
customize/cart/discount/payment code as Beverages and Featured (`openCustomize`,
`rebuildCustomizeBody`, `confirmCustomize`, `POSService.checkout`), so every bug already logged in
[QA-Validation-Beverages-Checkout.md](QA-Validation-Beverages-Checkout.md) — negative discount
increasing the total, dead $5/$10/$20 quick-cash buttons, silent invalid-discount fallback, no
Cancel Order confirmation — applies identically here and is not re-litigated below. This report
only covers what's specific to (or most visible on) the Bakery tab.

## Summary

| Result | Count |
|---|---|
| Passed | 2 |
| Bugs found (design/data-modeling) | 2 |
| Data coverage note (not a code bug) | 1 |

## Passed

1. **Real subcategories load correctly.** Bakery is backed by real `categories` rows (Sweet
   Treats, Grab & Go, Bagels & Muffins, Donuts & Donut Holes) and the item grid correctly groups
   items into on-screen sections (Cookies, Pastries, Cakes & Bars, Seasonal Sweets), confirmed
   live with real pricing.
2. **Full item → cart → checkout math is correct, including a split cash+card payment.** Verified
   live: Chocolate Chip Cookie, Small, Vanilla Swirl modifier → Subtotal $2.49, Tax $0.20, Total
   $2.69. Entered $2.00 cash (deliberately less than the total) — the app correctly detected the
   shortfall and offered a split payment; confirming it produced Order #183, Cash Applied $2.00,
   Card Charged $0.69, Change Due $0.00, all arithmetically correct ($2.69 − $2.00 = $0.69).

## Bugs found

### 1. Every Bakery item's customize screen shows drink-only options (Hot/Iced, Dairy/Sweetener, Flavor Swirl, espresso Add-Ons) — Medium

Reproduced live: tapping "Chocolate Chip Cookie" opens the same customize screen as a coffee
drink — a **Temperature: Hot / Iced** selector, a **Dairy/Sweetener** row (Regular - Cream &
Sugar, Black - No Cream, Cream Only, Sugar Only, Oat Milk), a **Flavor Swirl** row (Caramel,
Vanilla, Mocha), and an **Add-On** row (Extra Espresso Shot, Whipped Cream). None of these make
sense for a cookie, and the same happens for every other Bakery item (donuts, bagels, muffins,
cakes). Root cause: `allSizes` and `allModifiers` (`POSPanel.java` lines 121–122) are loaded
**once, globally**, from `pricingOptionsDAO.getAllSizes()`/`getAllModifiers()` at panel startup,
and `rebuildCustomizeBody()` renders all of them for every item with no per-item or
per-category filtering. This doesn't break pricing or checkout (selecting a nonsensical modifier
still adds its price correctly, as confirmed above), but it's a real UX/data-modeling gap — a
cashier could accidentally add "Extra Espresso Shot (+$0.50)" to a cookie, and a customer
reviewing the receipt would see it.

**Where:** `POSPanel.rebuildCustomizeBody()` (lines 1611–1667) and the shared `allSizes`/
`allModifiers` fields (lines 65–66, populated at 121–122).
**Suggested fix:** scope modifiers/sizes to a menu item's category (e.g. a
`category_modifier_groups` join table), or at minimum hide the Temperature row and
drink-oriented modifier groups when the current department isn't a beverage department.

### 2. Cart/receipt lines are always prefixed with "Hot" or "Iced", even for non-drink items — Low

Confirmed live: the cart line for the cookie above read "**Hot** Chocolate Chip Cookie". Root
cause: `CartItem.getDisplayName()` (`CartItem.java` lines 34–39) unconditionally prepends
`temperature` when it's non-null, and `temperature` is never null — `POSPanel.openCustomize()`
always initializes `pendingTemp = "Hot"` regardless of department, and `rebuildCustomizeBody()`
always renders the Hot/Iced row (see Bug 1), so every cart line and printed receipt for every
Bakery/Sandwiches/Retail item — not just Beverages — carries a meaningless "Hot"/"Iced" prefix.

**Where:** `CartItem.getDisplayName()`; upstream cause shared with Bug 1.
**Suggested fix:** once Bug 1's per-category filtering exists, only set/display `temperature` for
items in departments that actually have a hot/iced distinction.

## Data coverage note (not a code bug)

**Most (possibly all) Bakery items have no `recipe_ingredients` rows.** The checkout above
succeeded with no inventory check performed at all for the Chocolate Chip Cookie — confirmed by
code review: `OrderDAO`'s stock-check loop (`OrderDAO.java` line 68) queries
`recipe_ingredients` by `menu_item_id`, and if an item has no rows there, the loop simply has
nothing to iterate, so checkout proceeds without deducting or validating any ingredient stock.
This mirrors the Beverages report's Cold Brew finding but in the opposite direction: here the gap
is that inventory tracking may not exist at all for most of the Bakery menu, which is worth an
audit (are Bakery ingredients meant to be tracked, and if so, is `recipe_ingredients` just
unpopulated for this department?) before this is demoed as inventory-accurate.
