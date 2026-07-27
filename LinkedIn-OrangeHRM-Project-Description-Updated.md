# OrangeHRM — BDD Test Automation Framework (updated, under 2,000 chars)

Paste this entire block over your current OrangeHRM project description on LinkedIn (~1,565 characters, well under the 2,000 limit).

---

Built a multi-layer QA automation framework in Java, testing a live, publicly shared HR management application (OrangeHRM's open-source demo) across UI/BDD, REST API, accessibility, security headers, and performance — 72 automated tests, 100% pass rate.

Designed 50 Cucumber BDD scenarios across 11 feature files (Login, PIM, Admin, Leave, My Info, Time & Attendance, Recruitment, Buzz, Directory, Navigation/Dashboard) using Selenium WebDriver and Page Object Model, plus 22 TestNG tests covering boundary/equivalence partitioning, REST Assured API checks, security-header validation (clickjacking, XSS, cookie HttpOnly), axe-core accessibility scans, and performance timing. Used ChatGPT/Claude to brainstorm additional BDD scenarios and edge cases — each AI-suggested case was manually reviewed and validated before implementation.

Root-caused and fixed real defects instead of masking them with broader waits/retries — including a native-input clear() bug, a shared-account state-pollution issue in Time & Attendance, and a "reclick once if unresponsive" resilience pattern. Documented one genuine environment limitation with a dated @known-limitation tag instead of deleting or silently failing the test.

Reporting: TestNG, Cucumber HTML/JSON, ExtentReports with failure screenshots, and Allure.

Skills: Java · Selenium · Cucumber BDD · TestNG · REST Assured · POM · Maven · Allure · Accessibility Testing · API Testing · Security Testing · AI-Assisted Test Design

Link: github.com/REZAULKARIM2024/OrangeHRM-BDD-Test-Automation-Framework
