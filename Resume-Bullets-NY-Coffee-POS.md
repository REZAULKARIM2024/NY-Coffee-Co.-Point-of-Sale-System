# NY Coffee Co. POS — Resume / LinkedIn Bullet Points

Use these under a "Projects" section, or adapt into your experience bullets.

## Short version (2-3 bullets)

- Built and tested **NY Coffee Co. POS**, a Java Swing point-of-sale system with a MySQL backend, covering checkout, payroll, inventory, and delivery workflows.
- Designed and implemented a **30-test JUnit 5 suite** covering service-layer business logic (cart totals, tax calculation, payroll gross pay/withholding), plus live API integration tests using `java.net.http`.
- Built a **dependency-free REST API layer** (JDK `HttpServer`, hand-rolled JSON serializer) exposing menu, employee, order, and checkout endpoints; authored a full **Postman collection** with automated test scripts for functional and negative-path coverage (400/404 cases).

## Expanded version (for a project write-up / portfolio page)

**NY Coffee Co. Point of Sale System** — Java, MySQL, JUnit 5, REST API, Postman

- Developed a full-featured Java Swing POS application (menu browsing, cart/checkout, payroll, time clock, delivery queue, supplier management, multi-language UI) backed by MySQL via JDBC.
- Extracted core business logic (cart subtotal/tax calculation, payroll gross pay with overtime/weekend/holiday multipliers, tax withholding) into pure, testable service methods; wrote a **30-test JUnit 5 suite** achieving fast, DB-independent unit coverage.
- Built a **dependency-free REST API** on top of the existing service/DAO layer using the JDK's built-in `com.sun.net.httpserver.HttpServer` and a hand-rolled JSON parser/writer — no external framework dependencies — exposing `GET /api/health`, `/api/menu-items`, `/api/employees`, `/api/orders/{id}`, and `POST /api/checkout`.
- Authored a **Postman collection** with scripted assertions (status codes, response shape, happy-path and error-path checks) covering all endpoints, plus a `java.net.http`-based live integration test class that exercises the full checkout-to-order round trip against a running server and real database.
- Diagnosed and fixed environment-specific issues (encoding, port conflicts, Eclipse build-path misconfiguration) to get the full test suite running reliably both via command-line scripts and inside the IDE.
