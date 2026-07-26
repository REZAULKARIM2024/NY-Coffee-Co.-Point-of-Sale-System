# NY Coffee POS — Starter Build

A working Java Swing + MySQL point-of-sale foundation: login, role-based dashboard,
POS/cart/checkout with atomic inventory deduction, menu management, and inventory
management. The database schema also includes tables for every feature on your
roadmap (payroll, delivery, loyalty, suppliers) so you can build into them without
migrations later.

## What's actually working right now
- **Login** (`gui/LoginFrame`) — authenticates against `users` table, SHA-256 + salt.
- **Dashboard shell** (`gui/MainDashboard`) — sidebar nav, shows/hides screens by role
  (ADMIN/MANAGER/CASHIER).
- **POS panel** (`gui/POSPanel`) — category-tabbed menu grid, cart, discount, 8% tax,
  checkout. Checkout is atomic: order + line items + payment + ingredient stock
  deduction all happen in one DB transaction (`dao/OrderDAO.checkout`) — if stock is
  insufficient partway through, everything rolls back and you get a clear error.
- **Menu Management** (`gui/MenuManagementPanel`) — add/edit items, price/cost,
  active toggle, validated input.
- **Size & modifier pricing** — Size (Small/Medium/Large/Extra Large) and modifiers
  (milk options, flavor swirls, extra shots, etc.) each carry their own price delta
  in the `sizes` / `modifiers` tables. The customize screen loads them from the DB,
  lets staff multi-select modifiers, and `CartItem.getUnitPrice()` sums base price +
  size delta + every selected modifier's delta — that's the price actually charged
  and stored in `order_items.unit_price`.
- **Customer Loyalty Program** — "+ Loyalty Customer" button on the POS screen
  looks a customer up by phone (or creates them), shows their current point count,
  and on checkout awards +1 point. At 50 points the cheapest cart item becomes free
  and points reset to 0 (`POSService.checkoutWithLoyalty`, threshold configurable
  via `LOYALTY_POINTS_FOR_REWARD`).
- **Employees** (`gui/EmployeePanel`) — add/edit employee records (name, position,
  phone, email, hourly rate), plus "Create Login for Selected" which creates a
  `users` row tied to that employee via `UserDAO.createUser`.
- **Reports** (`gui/ReportsPanel`) — today's order count / total sales / tax
  collected / discounts given, plus a top-items-by-revenue table. Basic but real —
  pulls live from `orders` and `order_items`.
- **Inventory** (`gui/InventoryPanel`) — view stock + low-stock flag, manual
  adjustments (purchase-in/waste/correction) with an audit trail in
  `inventory_transactions`.
- **Time Clock** (`gui/TimeClockPanel`) — every login (all roles) clocks itself
  in/out against `time_clock`. Requires the login's `users.employee_id` to be
  set (see `EmployeePanel` → "Create Login for Selected"). Managers/admins also
  see a live feed of everyone's recent punches.
- **Payroll** (`gui/PayrollPanel`, manager/admin) — pick an employee + pay period,
  and `service/PayrollService` turns their closed time-clock shifts into
  regular/overtime/weekend/holiday hours and a gross/net pay figure, then saves
  it to `payroll_runs`. Rules: shifts on a date in the `holidays` table are paid
  at 1.5x; Sat/Sun shifts at 1.25x; weekday hours beyond 40/ISO-week are 1.5x
  overtime, the rest is regular pay. Multipliers and the OT threshold are
  constants at the top of `PayrollService` if you need to change them.
- **Delivery** — the POS cart now has Order Type (Dine In/Pickup/Delivery) and
  Source (In Store/Phone/Online) selectors; choosing Delivery prompts for an
  address and, on checkout, drops the order into `deliveries` as UNASSIGNED.
  `gui/DeliveryPanel` is the driver queue: Assign → Mark Picked Up → Mark
  Delivered, with a toggle to show completed deliveries too.
- **Recipes / prep instructions** (`gui/RecipePanel`) — numbered prep steps per
  menu item, per language, stored in `recipe_steps` (added by
  `database/migration_v3_recipe_steps.sql` if you're on an older DB). Everyone
  can read steps; managers/admins can add/edit/delete them. Language is a free-
  text field with `en`/`es`/`fr`/`zh` as presets — type any code you like.

## What's scaffolded but not built (tables exist, no screens yet)
Suppliers, Station/Kitchen Display, Receipt printing/email/SMS, real payment
gateway integration. See `database/schema.sql` for the `suppliers` table — the
rest of the build-order list below still applies to what's left.

## Setup

1. **Install MySQL**, then load the schema:
   ```
   mysql -u root -p < database/schema.sql
   ```
   This creates `pos_system` with 8 sample categories (Espresso & Coffee, Teas &
   More, Frozen Drinks, Sandwiches & More, Snacks & Wraps, Bagels & Muffins,
   Donuts, Brew At Home), menu items in each, and matching ingredients/recipes so
   checkout's stock deduction has something to deduct. If you already ran an
   earlier version of this schema, drop and re-import `pos_system` to pick up the
   new categories (this is dev/sample data, so safe to reset).

2. **Create your first login.** There's no seeded user yet. Easiest path:
   ```
   javac -d bin $(find src -name "*.java")
   java -cp bin com.possystem.util.PasswordUtil admin123
   ```
   This prints a `Salt` and `Hash`. Insert your admin row:
   ```sql
   INSERT INTO users (username, password_hash, salt, full_name, role)
   VALUES ('admin', '<paste Hash>', '<paste Salt>', 'Admin User', 'ADMIN');
   ```

3. **Get the MySQL connector.** Download `mysql-connector-j` (the `.jar`) from
   https://dev.mysql.com/downloads/connector/j/ and drop it in `lib/`.

4. **Import into Eclipse**: File → Import → Existing Projects into Workspace →
   select this folder. Add `lib/mysql-connector-j-*.jar` to the build path
   (right-click project → Build Path → Configure → Libraries → Add JARs).

5. **Set your DB credentials** in `src/com/possystem/config/DBConnection.java`
   (`DB_USER`, `DB_PASSWORD`).

6. **Run `Main.java`** as a Java Application.

## Suggested build order for the rest
1. **Suppliers** — CRUD on the `suppliers` table (already in the schema), plus
   optionally linking ingredients to a preferred supplier for reorder tracking.
2. **Station/Kitchen Display** — a read-only board driven by `order_items.station_status`
   (PENDING/PREPARING/READY/SERVED), filtered by each category's `station` column.
3. **Real payment gateway, receipt email/SMS** — `POSService.simulatePaymentReference`
   is the seam to swap in a real processor call.

Ask me to build any of these next and I'll write the full DAO + service + panel
for it, wired into what's already here.

## Note on this build
Payroll, Delivery, and multi-language Recipes above were just added. Also fixed
a compile bug in `POSPanel.java` (a bare ternary used as a statement, which isn't
legal Java even when both branches are method calls) — the whole `src/` tree now
compiles clean with `javac`.
