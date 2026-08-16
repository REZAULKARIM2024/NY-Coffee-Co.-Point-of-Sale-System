# QA Validation Report — About

**Scope:** End-to-end validation of the About page (`AboutPanel.java`, left sidebar) — the static
branding/info screen (leadership card, mission & vision, live "At a Glance" stats grid, and
support info) — combining static code review (`AboutPanel.java`, `SupplierDAO.java`,
`Supplier.java`, `EmployeeDAO.java`) with live functional testing.

## Summary

| Result | Count |
|---|---|
| Passed | 4 |
| Bugs found | 1 (Low) |

## Passed

1. **Owner photo loads correctly.** `loadCircularAvatar()` reads
   `/com/possystem/resources/owner.jpg` from the classpath and falls back to a "?" placeholder if
   the resource is missing or unreadable. Confirmed the resource file exists on disk and is
   compiled into `target/classes`, and confirmed live that the real photo renders correctly
   (cropped to a circle) — the fallback path never triggers in this build.
2. **The "At a Glance" stats grid loads live, correct data.** Confirmed live: Employees on File
   showed 103 (matches `EmployeeDAO.getAllEmployees()`, which is `SELECT * FROM employees` with
   no filter, so this count correctly includes the inactive "QA Test Employee" left over from the
   Employees QA pass — consistent with the label "on File" rather than "Active"), Menu Items
   showed 1546, and Today's date showed correctly as Aug 15, 2026. Each DAO call is wrapped in its
   own try/catch that silently defaults to 0 on failure, so a single broken count (e.g. a DB
   hiccup) can't take down the whole page — a reasonable defensive choice for a static info screen.
3. **The Support section's Knowledge Base path is accurate.** The page tells users to go to
   "Functions > SUPPORT > Knowledge Base." Verified live: the Functions tab's right-hand sidebar
   has a SUPPORT section containing an actual "Knowledge Base" button — the instructions match
   reality.
4. **The LinkedIn contact link is implemented safely (verified by code review).** `contactLine()`
   renders the LinkedIn line as an underlined, styled link with a hand cursor and opens it via
   `Desktop.getDesktop().browse(new URI(url))` on click, wrapped in a try/catch that shows a
   friendly "Couldn't open the link automatically" dialog with the raw URL if `Desktop.browse()`
   fails (e.g. no default browser configured) — a clean, non-crashing implementation. The phone
   number line correctly renders as plain, non-clickable text. Confirmed the link is styled and
   present on screen live; not clicked live, consistent with the standing policy of not following
   external links via automated browser/computer-use actions.

## Bugs found

### 1. The "Active Suppliers" stat label is misleading — there is no "active" concept for suppliers — Low

The stats grid shows a count labeled "Active Suppliers," but `Supplier.java` has no `active`
field at all, `database/schema.sql`'s `suppliers` table has no `active`/status column, and
`SupplierDAO.getAllSuppliers()` (`SELECT * FROM suppliers ORDER BY name`, no `WHERE` clause)
returns every supplier row unconditionally. The number shown is simply the total supplier count,
not a count of suppliers filtered by any real active/inactive status — the label implies a
distinction the system doesn't actually track. This is consistent with the Suppliers QA report,
which found the same DAO returns all rows with no status filtering anywhere in the app.

**Where:** `AboutPanel.java` `buildStatsGrid()` (label text "Active Suppliers" paired with
`new SupplierDAO().getAllSuppliers().size()`); `Supplier.java` (no `active` field);
`database/schema.sql` `suppliers` table (no status column).
**Suggested fix:** either relabel the stat to "Suppliers" / "Total Suppliers" to match what's
actually counted, or add a real `active` column to `suppliers` (mirroring the `employees` table's
`active` flag) and filter the count by it.

## Cross-reference note

This finding pairs with the Suppliers QA report's own observation that the Suppliers page and
schema have no concept of supplier status at all — About's mislabeled stat is a direct, visible
symptom of that same gap, surfacing it to every user who opens the About page rather than just to
someone reading the Suppliers table closely.

## Test data note

About is a fully read-only page — no forms, no CRUD, nothing was created, edited, or deleted
during this validation. No test data was introduced or needs cleanup.
