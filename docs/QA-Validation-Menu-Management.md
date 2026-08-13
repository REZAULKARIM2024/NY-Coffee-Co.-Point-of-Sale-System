# QA Validation Report — Menu Management

**Scope:** End-to-end validation of the Menu Management page (`MenuManagementPanel.java`) —
the admin screen used to add and edit menu items across every department — combining static
code review (`MenuManagementPanel.java`, `MenuItemDAO.java`) with live functional testing.

**Relationship to prior reports:** Menu Management is upstream of every POS/Checkout tab
already validated (Beverages through Functions): every item those reports tested was either
seeded directly in the database or could in principle be added/edited here. This report is
therefore the highest-leverage one so far — a bug here can silently break what any department
tab is able to sell.

## Summary

| Result | Count |
|---|---|
| Passed | 2 |
| Bugs found | 3 (1 Critical, 1 High, 1 Medium) |

## Passed

1. **Edit Selected correctly loads and saves existing item data.** Verified live: selecting
   "12-Cup Drip Brewer" and clicking Edit Selected opened a form pre-filled with its real
   Category (Coffee & Tea), Name, Price ($69.99), Cost ($34.00), and Active state — confirming
   the editor reads the selected row correctly rather than a stale/default state.
2. **The Active checkbox correctly controls POS visibility.** Confirmed by code review:
   `MenuItemDAO.getMenuItemsByCategory()` and `getActiveMenuItems()` both filter on
   `WHERE m.active = TRUE`, so unchecking Active in Menu Management does correctly hide an item
   from the POS grid (and `setActive()`, used by the Functions tab's Menu Item Availability
   button, updates the same flag).

## Bugs found

### 1. The Category dropdown mixes departments and subcategories with no distinction, and defaults to a department — new items can be silently unsellable — Critical

Reproduced live: opening **Add Item** shows a Category dropdown pre-selected to **"Featured"**
— which is a top-level department tab, not a real subcategory. The dropdown list itself
(`MenuManagementPanel.java` line 71, populated from `menuItemDAO.getAllCategories()`) mixes
department rows (Featured, Beverages, ...) and real subcategories (Breakfast Sandwiches, New
Arrivals, Local Items, Coffee & Tea, Coffee, Mugs & Drinkware, ...) in one flat, alphabetically-
unsorted-by-type list with no indentation or label to tell them apart.

To confirm the real-world impact, I added a test item ("QA Ghost Item Test", $1.00) with
Category left on **"Beverages"** (a department) and saved it. It saved successfully with no
warning. I then checked every subcategory sidebar item under the Beverages tab in POS/Checkout
(Coffee, Espresso, Teas, Refreshers, Frozen, Other Beverages) — the item appeared in **none of
them**. This is expected from the code: `POSPanel`'s department-tab handler
(`selectDepartmentTab`-equivalent code, lines 298–309) only ever calls
`showItemsForCategory()` for a department's **child** subcategories (via
`selectSubcategory(children.get(0))` and whichever child the cashier clicks); it never renders
items assigned directly to the department's own category id when that department has children.
The item becomes permanently invisible to every cashier and customer while still existing (and
still "Active") in the database.

**Where:** `MenuManagementPanel.java` line 71 (`categoryBox` populated from unfiltered
`getAllCategories()`) and the openEditor dialog generally (lines 64–126); root cause of the
invisibility itself is `POSPanel.java` lines 298–309 (department tabs with children never show
the parent category's own items).
**Suggested fix:** filter the Category dropdown to subcategories only (`getChildCategories()`
per department, or exclude rows where `parent_id IS NULL`unless a department has zero
children), and/or default the dropdown to no selection so a category must be explicitly chosen
before Save is enabled.

### 2. There is no Delete function anywhere in the app — a mis-categorized or unwanted item can never be removed, only deactivated — High (compounds Bug 1)

Confirmed by code review and live testing: `MenuManagementPanel` has only **Add Item** and
**Edit Selected** buttons — no Delete. The only related control anywhere in the app is
Functions → Menu Functions → **Menu Item Availability**, which (confirmed live) only flips the
`active` flag to `UNAVAILABLE`; it does not delete the row. This means the "QA Ghost Item Test"
item created to reproduce Bug 1 above can be deactivated (which I did, to leave the demo data
clean) but can never actually be removed from the `menu_items` table through the UI — the same
would be true for any real mis-entered item, typo'd duplicate, or discontinued product. Over
time this guarantees permanent clutter in the underlying data and in any report or export that
doesn't already filter on `active`.

**Where:** `MenuManagementPanel.java` (no delete button/handler exists); `MenuItemDAO.java` has
no `deleteMenuItem()` method.
**Suggested fix:** add a Delete button (soft-delete is fine given `active` already exists, but
expose a true delete option for accidental/duplicate entries, ideally gated behind a
confirmation dialog).

### 3. The item list has no search, filter, or sort controls despite listing every item in the system in one flat table — Medium

Confirmed live: **Add Item** / **Edit Selected**'s parent table (`loadTable()`,
`MenuManagementPanel.java` lines 49–62) calls `getAllMenuItems()`, which returns literally every
menu item across all seven departments in one `JTable` with no search box, filter dropdown, or
column-header sorting wired up — confirmed by scrolling through 20+ visible "Breakfast
Sandwiches" rows alone with many more categories below. Combined with Bug 1, this makes it hard
for an admin to notice a mis-categorized item after the fact, since there's no quick way to
filter down to "items under a specific department" or search by name to audit the catalog.

**Where:** `MenuManagementPanel.java`, `loadTable()` (lines 49–62) and constructor (lines 23–47).
**Suggested fix:** add a search field (filter by name) and/or a category filter dropdown above
the table, and enable column sorting on the `JTable` (`table.setAutoCreateRowSorter(true)`).

## Note: editor is missing several fields (not a bug, a gap)

The Add/Edit dialog only exposes Category, Name, Price, Cost, and Active. It has no field for
**Section** (so every new item is added with `section = NULL` and appears under its own
category-name header rather than blending into an existing named section like "Cold Brew"),
**Barcode** (so a new item can never be found by Functions → Barcode Entry), **Description**, or
**Image Path**. None of this breaks existing data — editing an existing item leaves those fields
untouched — but an admin cannot set any of them for a brand-new item without direct database
access. Worth a future enhancement alongside the fixes above.
