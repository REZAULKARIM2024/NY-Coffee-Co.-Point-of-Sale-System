# QA Validation Report — Inventory

**Scope:** End-to-end validation of the Inventory page (`InventoryPanel.java`) — the ingredient
stock tracking and manual adjustment screen — combining static code review
(`InventoryPanel.java`, `IngredientDAO.java`, `Ingredient.java`, `OrderDAO.java`) with live
functional testing.

**Relationship to prior reports:** Inventory tracks raw ingredients (e.g. Espresso Beans, Bread,
Bacon), not menu items, so it's a different data model from every POS/Checkout tab and from
Menu Management. It connects to the rest of the app in one important way: `OrderDAO.checkout()`
looks up each sold menu item's `recipe_ingredients` and automatically deducts the matching
ingredient stock as part of the same order transaction. This report evaluates both that
automatic path and the manual "Adjust Stock" tool that is Inventory's only other feature.

## Summary

| Result | Count |
|---|---|
| Passed | 4 |
| Bugs found | 3 (1 Critical, 1 High, 1 Low/Medium) |

## Passed

1. **Sales correctly deduct ingredient stock in real time, with insufficient-stock protection.**
   Confirmed live end-to-end: before checkout, Espresso Beans stood at 2598.00 g. Ordering one
   Small Hot Original Coffee and completing checkout with CASH (Order #187) dropped Espresso
   Beans to exactly 2468.00 g — a clean 130 g deduction matching the item's recipe, logged as a
   `SALE`-reason row in `inventory_transactions` alongside the order id. Code review
   (`OrderDAO.checkoutMultiPayment()`, lines 104–136) confirms this runs inside the same DB
   transaction as the order itself, and separately throws `OrderDAO.InsufficientStockException`
   — caught by `POSPanel` (lines 2222–2224) and shown as a clear "Insufficient stock" dialog — if
   any ingredient doesn't have enough stock, blocking the sale rather than allowing it to
   oversell inventory.
2. **Non-numeric adjustment amounts are correctly rejected.** Typing "abc" into the Amount field
   and pressing OK produced "Amount must be a number." with no database change — verified live.
3. **Adjust Stock correctly requires a row to be selected first.** Clicking Adjust Stock with no
   ingredient selected produced "Select an ingredient first." with no dialog opened — verified
   live.
4. **Low Stock highlighting is accurate**, including at the extremes: the table's "Low Stock?"
   column and red row tint correctly triggered the moment a test adjustment (see Bug 1) pushed
   an ingredient's stock below its threshold, including into negative territory.

## Bugs found

### 1. Stock adjustments have no floor check — an ingredient's stock can be driven arbitrarily negative with one click — Critical

Reproduced live: Bread had 38.00 units on hand (threshold 10.00). Opening Adjust Stock, selecting
reason WASTE, and entering **-1000** was accepted immediately with no warning, dropping Bread's
stock to **-962.00** — correctly flagged red/"YES" in Low Stock, but a negative physical
quantity is meaningless in real life and signals either a fat-fingered amount (an extra zero) or
a genuine process failure that nothing in the UI catches. `IngredientDAO.adjustStock()`
(`IngredientDAO.java` line 28) runs a bare `UPDATE ingredients SET stock_quantity =
stock_quantity + ?` with no check that the result stays at or above zero, and
`InventoryPanel.openAdjustDialog()` (lines 83–113) performs no client-side bound either. Left
uncorrected, a single mistaken adjustment could make an ingredient's on-hand count permanently
wrong, which would in turn make future `InsufficientStockException` checks at checkout
(Passed #1) unreliable, since they compare against this same, possibly-corrupted number. (Test
data was restored to 38.00 immediately after reproducing this.)

**Where:** `IngredientDAO.java`, `adjustStock()` (line 28); `InventoryPanel.java`,
`openAdjustDialog()` (lines 83–113) — no validation between reading the typed amount and calling
`adjustStock()`.
**Suggested fix:** before applying the update, either reject adjustments that would take
`stock_quantity` below zero (with a clear error), or require manager confirmation for any
adjustment large enough to do so — mirroring the safeguard `OrderDAO` already applies at
checkout time.

### 2. The adjustment "Reason" is a free-standing label — nothing ties its sign to the amount actually typed — High

Reproduced live: selected ingredient Bacon (903.00 g on hand), opened Adjust Stock, chose Reason
**PURCHASE_IN** (which by its name means stock coming in), but typed **-50** in Amount and
pressed OK. The app accepted it without complaint: Bacon's stock dropped to 853.00 g, and the
resulting `inventory_transactions` row records `reason = 'PURCHASE_IN'` for what was actually a
50 g reduction — the exact same mismatch would work in reverse (logging a stock increase as
"WASTE"). `InventoryPanel.java`'s own code comment at line 106 acknowledges this design
("let the sign the user enters drive it directly"), but it means the audit trail this screen
exists to produce can be trivially wrong, whether by accident (picking the wrong item from a
3-option dropdown) or by design (deliberately mislabeling shrinkage as a purchase to hide a
discrepancy). (Test data was restored to 903.00 g with a correctly-signed PURCHASE_IN adjustment
immediately after reproducing this.)

**Where:** `InventoryPanel.java`, `openAdjustDialog()` (lines 83–113), specifically the comment
and logic at lines 105–108.
**Suggested fix:** tie the sign to the reason automatically (PURCHASE_IN always adds, WASTE and
CORRECTION-down always subtract, with a separate "CORRECTION_UP" option if positive corrections
are needed), or at minimum warn/block when the typed sign contradicts the selected reason.

### 3. No confirmation step scales with the size of the adjustment, and there's no way to view adjustment history from this screen — Low/Medium

Every adjustment, whether ±1 or ±1000, goes through the exact same single OK click with no
"are you sure" step — combined with Bug 1 this makes a catastrophic fat-finger mistake just as
easy as a routine one. Separately, `IngredientDAO` and `OrderDAO` both write every adjustment and
every sale-driven deduction to `inventory_transactions` (confirmed by code — `IngredientDAO.java`
line 29, `OrderDAO.java` line 70), but no query anywhere in the codebase ever reads that table
back, and `InventoryPanel` has no history/log view — so once an adjustment is made, there is no
way inside the app to see who made it, when, or why, even though the data is being collected.

**Where:** `InventoryPanel.java` (no confirmation dialog scaled to magnitude; no history view);
`inventory_transactions` is write-only across the codebase (`IngredientDAO.java` line 29,
`OrderDAO.java` line 70 — no corresponding SELECT anywhere).
**Suggested fix:** add a confirmation dialog for adjustments beyond a sensible threshold (e.g.
larger than current stock, or beyond a configurable magnitude), and add a simple read-only
"Recent Adjustments" panel or report sourced from `inventory_transactions` so the audit data
being collected is actually usable.
