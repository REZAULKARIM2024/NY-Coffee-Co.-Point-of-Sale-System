# NY Coffee Co. — Point of Sale System (updated, ~1,370 chars)

Paste this over your current project description on LinkedIn.

---

Designed and built a full-featured Java Swing point-of-sale system for a coffee shop chain, covering checkout, payroll, time clock, inventory, delivery, supplier management, and multi-language support — backed by MySQL.

Extracted core business logic (cart totals, tax calculation, payroll gross pay with overtime/weekend/holiday multipliers, tax withholding) into pure, testable service methods and wrote a 30-test JUnit 5 suite for fast, DB-independent coverage. Migrated the project to a standard Maven build (pom.xml-driven dependencies, compiler/surefire/allure-maven plugins) and integrated Allure for structured, shareable test reports.

Built a dependency-free REST API on top of the existing service layer using the JDK's built-in HttpServer and a hand-rolled JSON parser — no external framework dependencies — exposing endpoints for menu items, employees, orders, and checkout. Authored a full Postman collection with scripted assertions covering both happy-path and error-path (400/404) cases, plus a live HTTP integration test class validating the full checkout-to-order flow against a running server and real database. Logged and tracked defects found during testing in Jira.

Skills: Java · MySQL · JUnit 5 · Maven · Allure · REST API Design · Postman · JDBC · SQL · Jira

Link: github.com/REZAULKARIM2024/NY-Coffee-Co.-Point-of-Sale-System
