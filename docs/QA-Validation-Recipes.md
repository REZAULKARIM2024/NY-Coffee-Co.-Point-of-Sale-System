# QA Validation Report — Recipes

**Scope:** End-to-end validation of the Recipes page (`RecipePanel.java`) — the prep-instruction
viewer/editor that lets staff read numbered recipe steps for any menu item in a chosen language,
and lets managers/admins add, edit, or delete those steps — combining static code review
(`RecipePanel.java`, `RecipeDAO.java`, `RecipeStep.java`, `MenuItemDAO.getAllMenuItems()`) with
live functional testing.

**Relationship to prior reports:** Recipes is independent of the other modules already
validated — it stores per-item, per-language prep instructions (`recipe_steps` table), which is
a separate concept from Inventory's `recipe_ingredients` (used for stock deduction at checkout).
This report only covers the Recipes screen itself.

## Summary

| Result | Count |
|---|---|
| Passed | 3 |
| Bugs found | 4 (1 Critical, 1 High, 2 Medium) |

## Passed

1. **Add / Edit / Delete Step all work correctly end-to-end, including persistence.** Verified
   live: added a new step and saw it appear immediately; edited an existing step's text (appended
   " QAEDIT", saved, confirmed the change rendered, then edited again to restore the original
   text) and confirmed `RecipeDAO.updateStepText()` genuinely persists changes rather than just
   updating the in-memory list; deleted a step and saw it disappear immediately, backed by
   `deleteStep()`.
2. **Validation and guard messages work correctly.** Confirmed live: clicking Add Step and
   submitting a blank instruction correctly shows "Instruction text is required." and does not
   save; clicking Edit Selected or Delete Selected with no step highlighted correctly shows
   "Select a step first." instead of throwing an error; Delete Selected always shows a
   "Delete step N?" Yes/No confirmation dialog before actually deleting.
3. **Edit/Add/Delete buttons are correctly restricted to managers and admins.** Confirmed by code
   review: `RecipePanel`'s constructor only calls `add(buildEditButtons(), ...)` when
   `currentUser.isManagerOrAbove()` is true, so a cashier-level account never sees or can reach
   these buttons at all — unlike the Functions tab (flagged in an earlier report), this is a
   real, working role restriction, not just a client-side label.

## Bugs found

### 1. Deleting a step leaves a permanent gap in the numbering, and new steps never fill it — Critical

Reproduced live: the "12-Cup Drip Brewer" recipe started with 3 clean steps (Step 1, 2, 3).
Deleting Step 2 immediately left the list showing **Step 1** and **Step 3** — no step 2, and
nothing renumbers automatically. Adding a brand-new step afterward made it **Step 4**, not a new
Step 2 — the gap is permanent and only grows over time as steps are edited.

The root cause is in `RecipeDAO.java`: `deleteStep()` (lines 83–92) is a bare
`DELETE FROM recipe_steps WHERE id = ?` with no follow-up renumbering of the remaining rows, and
`addStep()` (lines 46–69) always computes the next number as `MAX(step_number) + 1` for that
item+language, so it can never fill an earlier gap. Since `RecipePanel` displays each step
prefixed with its literal `step_number` ("Step 1:", "Step 3:", "Step 4:"), kitchen/prep staff
reading this screen at the station will see a recipe that looks like it's missing a step, which
is confusing and undermines trust in the numbering — especially for recipes that get corrected
occasionally over their lifetime.

**Where:** `RecipeDAO.java` lines 46–69 (`addStep()`, always appends via `MAX+1`) and lines 83–92
(`deleteStep()`, no renumbering).
**Suggested fix:** after a delete, renumber the remaining steps for that item+language
sequentially (e.g. `UPDATE recipe_steps SET step_number = step_number - 1 WHERE menu_item_id = ?
AND language = ? AND step_number > ?`), inside the same operation so the numbering always stays
contiguous.

### 2. Recipe language field accepts anything, silently fragmenting content across typo'd language codes — High

Reproduced live: the Language field is an *editable* `JComboBox` — typing "ENG" (capital, a
plausible typo for "en") and pressing Enter switched the view to a completely empty list, even
though "en" already had steps for the same item. Adding a step while "ENG" was selected saved it
successfully under that separate bucket. There is no validation anywhere in `selectedLanguage()`
(`RecipePanel.java` lines 101–105) beyond a `trim().toLowerCase()` — no check against a known set
of language codes, and no warning that "ENG" and "en" are different keys in the database.

This is made worse by the fact that `RecipeDAO.getLanguagesForItem()` — a method that would let
the UI show "this item already has steps in: en, es" — is fully implemented but **never called
from `RecipePanel` or anywhere else in the codebase** (confirmed via project-wide search). So a
manager who fat-fingers the language field has no way to notice the mistake: the step silently
saves, and nothing in the app will ever surface that an orphaned "ENG" (or "Eng", "en ", etc.)
bucket exists alongside the real "en" steps.

**Where:** `RecipePanel.java` lines 101–105 (`selectedLanguage()`, no validation);
`RecipeDAO.java` lines 30–43 (`getLanguagesForItem()`, dead code — implemented but never
invoked).
**Suggested fix:** make the language field a strict dropdown (not editable) populated from a
fixed list of supported codes, and/or call `getLanguagesForItem()` on item selection to show which
languages already have content, catching typos before they create orphaned data.

### 3. Recipe language options don't match the app's actual supported languages — Medium

Confirmed by code review: `RecipePanel.java` line 26 hardcodes the language dropdown's default
options as `{"en", "es", "fr", "zh"}`. But the rest of the app — including the Help & Support
dialog used throughout this QA session — officially supports five languages: English, Bangla,
Hindi, Spanish, and French (`HelpDialog.java`'s `TOPICS_EN/BN/HI/ES/FR` arrays). Bangla ("bn")
and Hindi ("hi"), two of the app's five supported languages, are missing from the Recipes
default list entirely, while Chinese ("zh") — not supported anywhere else in the application —
is offered by default. A manager wanting to add Bangla or Hindi prep instructions (reasonable,
given the app's own language support) would have to know to type "bn"/"hi" manually with no
prompt that these are valid options.

**Where:** `RecipePanel.java` line 26 (`languageBox` default items).
**Suggested fix:** align the default language list with the app's real supported set
(`en, bn, hi, es, fr`), sourced from the same place `HelpDialog`/`I18n` define supported
languages, so the two stay in sync.

### 4. Menu Item dropdown has no search or filter, and is very long — Low/Medium

Confirmed live: opening the Menu Item dropdown shows a plain alphabetically-ish flat list drawn
from `MenuItemDAO.getAllMenuItems()` — every menu item in the entire catalog, across every
department, with no grouping by category and no way to type-to-search. Given the app's catalog
was earlier expanded to 50–100+ items per subcategory across seven departments, finding one
specific item (e.g. one particular sandwich variant) means scrolling through a very long
unfiltered list — this is the same class of usability gap noted for Menu Management's item table
in an earlier report.

**Where:** `RecipePanel.java` lines 68–78 (`buildSelectorRow()`, plain non-editable `JComboBox`
populated from all menu items); `MenuItemDAO.java` lines 102–114 (`getAllMenuItems()`, no
filtering).
**Suggested fix:** make the item combo box editable/searchable (Swing autocomplete), or add a
department/category filter above it, mirroring the POS's own department-tab structure.

## Test data note

All QA test artifacts were cleaned up where possible: the stray "ENG"-language step and the
extra "QA test new step after deletion" entry were both deleted, and the edited step's text was
restored to its original wording. The one exception is the "12-Cup Drip Brewer" (en) recipe's
step numbering, which now permanently reads Step 1, Step 3 with no Step 2 — this could not be
restored through the UI, since there is no way to renumber steps manually. This residual gap is
itself live evidence supporting Bug 1 above.
