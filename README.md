# NY Coffee Co. — Point of Sale System

A full-featured, coffee-shop-branded point-of-sale application built with Java Swing and MySQL. It covers the counter (order entry, customization, payments, loyalty) as well as the back office (inventory, payroll, HR, delivery, suppliers, reporting) in a single desktop app styled after a real-world NYC POS terminal.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)
![UI](https://img.shields.io/badge/UI-Java%20Swing-informational)
![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen)
![License](https://img.shields.io/badge/License-Demo%2FInternal-lightgrey)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Default Login](#default-login)
- [Project Structure](#project-structure)
- [Database Overview](#database-overview)
- [Seed / Utility Scripts](#seed--utility-scripts)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Overview

NY Coffee Co. POS is a single-store terminal application: a cashier logs in, rings up orders from a department-tabbed menu (Beverages, Featured, Bakery, Sandwiches, Retail, Local), takes payment (cash, card, gift card, or split tender), and the system handles everything downstream — inventory deduction, order source/type tracking, delivery queueing, and loyalty points — inside atomic database transactions. Managers and admins get additional screens for staffing, payroll, purchasing, and reporting.

## Features

### Point of Sale
- Department-tabbed menu grid (Beverages, Featured, Bakery, Sandwiches, Retail, Local) with a subcategory sidebar, mirroring a real counter-service POS layout.
- Per-item customization screen: size (Small/Medium/Large/Extra Large, each with its own price delta), hot/iced, dairy & sweetener options, flavor swirls, and add-ons — all priced from the `sizes` / `modifiers` tables and summed live.
- Cart with discount entry, live subtotal/tax/total, and order type (Dine In / Pickup / Delivery) + source (In Store / Phone / Online) selectors.
- **Payments screen**: on-screen keypad, Credit Card / CASH / Gift Card Redeem.
  - Cash sales require the cashier to key in what the customer handed over; the app validates it covers the total and automatically calculates and displays **change due**.
  - **Split tender**: if the cash handed over falls short, the app offers to charge the remaining balance to a credit card in the same transaction, recording both legs as separate payment records against the order.
- **Customer loyalty program**: look up or register a customer by phone, +1 point per order, and at 50 points the cheapest cart item is automatically comped and points reset.
- Barcode entry, gift card redemption flow, and a full **Functions** tab covering manager/cashier operations: drawer paid-in/paid-out/cash pull/no-sale, till assignment & counting, safe management, order adjustments, price overrides, manual credit entries, and phone/DT/OTG order handling.

### Back Office
- **Menu Management** — add/edit items, pricing, cost, and availability.
- **Inventory** — live stock levels with low-stock flagging, manual adjustments (purchase-in/waste/correction) with a full audit trail.
- **Time Clock** — clock in/out per employee; managers see a live feed of recent punches.
- **Payroll** — pick an employee and pay period; hours are pulled from time clock punches and split into regular/overtime/weekend/holiday buckets, with federal, Social Security, Medicare, state, and city tax withholding calculated automatically. Every completed run can generate:
  - an itemized **paystub** (earnings, deductions, YTD totals) with a print dialog, and
  - a bank-check-style **paycheck** (with amount spelled out in words) with its own print dialog.
- **Delivery Queue** — orders marked for delivery drop into a queue; drivers get assigned, mark picked up, and mark delivered.
- **Recipes** — numbered prep steps per menu item, per language, for staff training and consistency.
- **Reports** — live sales, tax, and discount totals plus top items by revenue, alongside deeper financial/sales/employee reports under Functions.
- **Employees** — manage staff records and create their POS logins.
- **Suppliers** — manage vendor contacts for purchasing and restocking.
- **About** — company info, leadership profile, mission & vision, and live at-a-glance stats pulled from the database.

### Platform
- Role-based access (ADMIN / MANAGER / CASHIER) with SHA-256 + salt authentication.
- 5-language support (English, Bangla, Hindi, Spanish, French) with an in-app language switcher and help dialog.
- NYC-themed branding throughout (skyline art, coffee-shop illustration, color-coded navigation per section).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI | Java Swing |
| Database | MySQL 8.x |
| DB Driver | MySQL Connector/J 9.7.0 |
| Printing | `java.awt.print` (native OS print dialog) |
| Build/IDE | Eclipse (or any IDE with a standard `src`/`lib` layout) |

## Prerequisites

- JDK 17 or later
- MySQL Server 8.x
- An IDE (Eclipse recommended) or a plain `javac`/`java` toolchain
- Windows, macOS, or Linux (batch launcher scripts are Windows-specific; other platforms can run the compiled classes directly)

## Installation

1. **Clone the repository**
   ```bash
   git clone <this-repo-url>
   cd ny-coffee-pos
   ```

2. **Install MySQL and load the schema**
   ```bash
   mysql -u root -p < database/schema.sql
   ```
   This creates the `pos_system` database with categories, menu items, sizes/modifiers, and supporting sample data.

3. **Get the MySQL connector.** The driver jar is already vendored under `lib/mysql-connector-j-9.7.0/`. To use a different version, download it from https://dev.mysql.com/downloads/connector/j/ and update the classpath references.

4. **Import into your IDE.** In Eclipse: File → Import → Existing Projects into Workspace → select this folder, then add `lib/mysql-connector-j-9.7.0/.../mysql-connector-j-9.7.0.jar` to the build path (right-click project → Build Path → Configure Build Path → Libraries → Add JARs).

## Configuration

Set your database credentials in `src/com/possystem/config/DBConnection.java`:

```java
DB_URL      = "jdbc:mysql://localhost:3306/pos_system"
DB_USER     = "root"
DB_PASSWORD = "<your password>"
```

## Running the Application

- **From an IDE:** run `com.possystem.Main` as a Java Application.
- **From the command line (Windows):** double-click or run `run_app.bat`, which compiles/launches with the classpath already wired to `target/classes` and the MySQL driver.
- **From the command line (manual):**
  ```bash
  javac -d target/classes -cp lib/mysql-connector-j-9.7.0/mysql-connector-j-9.7.0/mysql-connector-j-9.7.0.jar $(find src -name "*.java")
  java -cp "target/classes;lib/mysql-connector-j-9.7.0/mysql-connector-j-9.7.0/mysql-connector-j-9.7.0.jar" com.possystem.Main
  ```

## Default Login

No user is seeded by default. Create your first admin account by hashing a password with the built-in utility and inserting the row yourself:

```bash
java -cp target/classes com.possystem.util.PasswordUtil admin123
```

This prints a `Salt` and `Hash`. Insert your admin row:

```sql
INSERT INTO users (username, password_hash, salt, full_name, role)
VALUES ('admin', '<paste Hash>', '<paste Salt>', 'Admin User', 'ADMIN');
```

Alternatively, run the provided seed scripts (see [Seed / Utility Scripts](#seed--utility-scripts)) to populate sample employees, payroll history, deliveries, recipes, and suppliers.

## Project Structure

```
src/com/possystem/
  config/     DB connection handling
  model/      Domain objects (Employee, MenuItem, Order, PayrollRun, ...)
  dao/        JDBC data-access layer (one per entity/feature area)
  service/    Business logic (POSService, PayrollService, ...)
  gui/        Swing screens (POSPanel, PayrollPanel, AboutPanel, ...)
  util/       Shared UI theming, i18n, printable art, helpers
  tools/      Standalone seed/migration utilities (run via run_*.bat)
database/
  schema.sql  Full schema + sample seed data
lib/
  mysql-connector-j-9.7.0/   JDBC driver jar
target/
  classes/    Compiled output
```

## Database Overview

Key tables in `pos_system`:

| Table | Purpose |
|---|---|
| `categories`, `menu_items`, `sizes`, `modifiers` | Menu structure and pricing |
| `orders`, `order_items`, `payments` | Order lifecycle and (possibly split) payments |
| `ingredients`, `recipe_ingredients`, `inventory_transactions` | Stock tracking and deduction |
| `employees`, `users`, `time_clock`, `payroll_runs` | HR, auth, and payroll |
| `customers` | Loyalty program |
| `deliveries` | Delivery queue |
| `recipe_steps` | Multi-language prep instructions |
| `suppliers` | Vendor/purchasing contacts |

See `database/schema.sql` for the complete definition and sample data.

## Seed / Utility Scripts

The repo includes standalone launchers (`run_*.bat`) for one-off data setup and maintenance tasks — seeding employees/payroll/deliveries/recipes/suppliers/barcodes, and migration tools for schema changes (e.g. payroll tax columns, operations tables). Each corresponds to a `main()` class under `src/com/possystem/tools/`.

## Roadmap

- Real payment gateway integration (currently simulated in `POSService.simulatePaymentReference`)
- Receipt printing/email/SMS
- Kitchen/station display board
- Automated test coverage (unit tests for `service`/`dao` layers, integration tests for checkout flows)

## Contributing

This is currently a single-owner project. Issues and pull requests are welcome — please open an issue describing the change before submitting a PR so the approach can be discussed first.

## License

Internal/demo project — not licensed for production payment processing. `POSService` payment references are simulated; see the code for the integration seam if wiring in a real payment gateway.

## Contact

**Rezaul Karim** — NY Coffee Co.
[LinkedIn](https://www.linkedin.com/in/rezaul-karim-803a3b273)
