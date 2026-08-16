# QA Validation Report — Suppliers

**Scope:** End-to-end validation of the Suppliers page (`SupplierPanel.java`, left sidebar) — the
supplier contact directory plus Add Supplier, Edit Selected, Delete Selected, and Refresh —
combining static code review (`SupplierPanel.java`, `SupplierDAO.java`, `Supplier.java`,
`database/schema.sql`) with live functional testing.

## Summary

| Result | Count |
|---|---|
| Passed | 2 |
| Bugs found | 2 (1 Medium, 1 Low) |

## Passed

1. **Full Add/Edit/Delete CRUD works correctly and leaves no residue when cleaned up.**
   Reproduced live end-to-end: added "QA Test Supplier" (validated correctly), edited it to add
   a Contact Person ("QA Tester", confirmed the change persisted and displayed immediately),
   then selected it and used Delete Selected. Suppliers is the only page found across this
   entire QA effort with a genuine, working Delete function — it shows a clear
   "Delete supplier \"X\"?" Yes/No confirmation, and on Yes performs a real hard `DELETE FROM
   suppliers` that immediately disappears from the table with no orphaned data. All prior
   pages reviewed (Employees, Recipes, Menu Management, etc.) have no delete function at all, so
   this a meaningfully more complete implementation.
2. **Add/Edit validation works correctly.** Reproduced live: an empty Supplier Name is rejected
   ("Supplier name is required."), and an invalid phone like "abc" is rejected ("Enter a valid
   phone number or leave it blank.") before anything is saved.

## Bugs found

### 1. A validation failure in Add/Edit Supplier discards the whole form — Medium

Reproduced live, the same pattern documented across every other management page in this app:
typing a Supplier Name plus an invalid Phone and pressing OK correctly shows the validation
error, but clicking OK on that message closes the entire Add/Edit Supplier dialog — the Supplier
Name already typed is lost and the form must be filled out again from a blank state.

**Where:** `SupplierPanel.java` `openEditor()` (lines 104–122): one
`JOptionPane.showConfirmDialog(...)` call followed by sequential validation checks that each
`return` out of the method after the dialog has already closed.
**Suggested fix:** loop and re-show the same form (with prior field values preserved) on
validation failure, only closing on a valid submit or explicit Cancel — consistent with the fix
suggested for the same issue in the Employees, Recipes, and Payroll QA reports.

### 2. Suppliers has no functional connection to Inventory or purchasing anywhere in the app — Low

Confirmed by code review and live testing: despite `SupplierPanel.java`'s own code comment
describing it as being "used for purchasing and inventory restocking," there is no link between
a supplier and an ingredient anywhere in the system. `database/schema.sql`'s `ingredients`
table (and every other table) has no `supplier_id` column, `SupplierDAO`/`Supplier.java` have no
relationship fields, and live testing of Inventory's "Adjust Stock" dialog — including its
PURCHASE_IN reason, which is exactly the restocking scenario a supplier link would matter for —
showed no Supplier field or picker at all. Suppliers today is a fully standalone contact
directory: useful for looking up a phone number or address, but disconnected from the
purchasing/restocking workflow it was clearly intended to support.

**Where:** `database/schema.sql` (no `supplier_id` anywhere); `SupplierDAO.java`/`Supplier.java`
(no relationship fields); `InventoryPanel.java`'s Adjust Stock dialog (no Supplier field,
confirmed live).
**Suggested fix:** add a `supplier_id` column to `ingredients`, expose a Supplier picker in
Inventory's Adjust Stock dialog (at least for the PURCHASE_IN reason), and optionally show a
"supplied ingredients" list on the Suppliers page itself.

## Test data note

The one test supplier created during live testing ("QA Test Supplier", contact "QA Tester") was
fully deleted using the page's own Delete Selected function as part of the test — this is the
only QA report in this series with genuinely no leftover test data, since Suppliers is the only
page with a working delete.
