# QA Validation Report — POS / Checkout: Featured

**Scope:** End-to-end validation of the Featured tab (subcategories, the client-side "News &
Promos" info board, item customization, cart, and checkout), combining static code review
(`POSPanel.java`) with live functional testing.

**Relationship to the Beverages report:** Featured items flow through the exact same
customize/cart/discount/payment code as Beverages (`openCustomize`, `confirmCustomize`,
`POSService.checkout`), so every bug already logged in
[QA-Validation-Beverages-Checkout.md](QA-Validation-Beverages-Checkout.md) — negative discount
increasing the total, dead $5/$10/$20 quick-cash buttons, no Hot/Iced selected-state, silent
invalid-discount fallback, no Cancel Order confirmation — applies identically here and is not
re-litigated below. This report only covers what's specific to the Featured tab.

## Summary

| Result | Count |
|---|---|
| Passed | 2 |
| Bugs found (Featured-specific) | 2 |

## Passed

1. **Real subcategories load correctly.** Unlike a placeholder tab, Featured is backed by real
   `categories` rows (New Arrivals, Fan Favorites, Limited Time Offers, Value Bundles), each
   grouping real menu items by section (Refreshers, Lattes & Espresso, Matcha, Lemonade, ...),
   confirmed live.
2. **Full item → cart → checkout math is correct for a Featured item.** Verified live: Perfect
   Matcha $4.75 (Small, no modifiers) → Subtotal $4.75, Tax $0.38, Total $5.13 → checked out with
   $60 cash tendered → Order #182 completed, Change Due $54.87, all arithmetically correct. No
   inventory/recipe issue on this item (unlike the Cold Brew data problem found in the Beverages
   report).

## Bugs found (Featured-specific)

### 1. "News & Promos" never gets sidebar highlight feedback — Low/Medium

Every other sidebar button (real subcategories, and Beverages' equivalent) turns the active color
when selected, via `highlightSidebarItem()`. The synthetic "News & Promos" button is wired
straight to `showFeaturedNews()` and never calls `highlightSidebarItem("News & Promos")` — so
after clicking it, no sidebar button shows as selected at all (confirmed live: neither "New
Arrivals" nor "News & Promos" was highlighted while viewing the promos board). A cashier glancing
at the sidebar has no visual confirmation of which screen is currently showing.

**Where:** `POSPanel.rebuildSidebar()`, the `newsBtn.addActionListener` block (around line 1190).

### 2. Switching language while viewing "News & Promos" silently navigates away from it — Medium

Reproduced live: with "News & Promos" open (This Month's Happenings board), switching the app
language from English to Bangla immediately replaced the promos board with the item grid for
whatever real subcategory was last selected ("New Arrivals"), with no indication anything
navigated. Root cause: `showFeaturedNews()` never updates `currentSubcategory`, and
`refreshLanguage()`'s re-render branch unconditionally calls
`showItemsForCategory(currentSubcategory)` whenever `currentDepartment != null` and no
Functions/Payments group is active — it has no way to know the on-screen content was actually the
info board, not an item grid. Low real-world impact (language switching mid-order is rare) but a
clean repro of a state-tracking gap: the "currently displayed screen" isn't tracked as its own
piece of state, only inferred from `currentSubcategory`/`activeFunctionsGroup`/`activePaymentsGroup`,
and the News & Promos board falls through that inference entirely.

**Where:** `POSPanel.refreshLanguage()` (the `else if (currentDepartment != null)` branch) and
`POSPanel.showFeaturedNews()` (doesn't set any tracking field before rendering).

**Suggested fix:** track the info-board state explicitly (e.g. a `boolean showingFeaturedNews`
flag, mirroring `activeFunctionsGroup`/`activePaymentsGroup`), check it first in
`refreshLanguage()`, and call `highlightSidebarItem("News & Promos")` from the button's listener.
