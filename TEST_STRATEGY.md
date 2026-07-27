# Test Strategy — NY Coffee Co. POS

This document explains *why* the test suite is shaped the way it is, not just what's in it. The
README's [Testing](README.md#testing) and [BDD & E2E Testing](README.md#bdd--e2e-testing-cucumber-selenium)
sections cover mechanics (how to run things); this covers the reasoning behind the choices.

## Test pyramid

```
        /\
       /  \        Selenium E2E (2 tests)      — one real browser flow, the /admin dashboard
      /----\
     /      \       Cucumber BDD (5 scenarios)  — stakeholder-readable REST API contracts
    /--------\
   /          \      Live HTTP integration (4)   — ApiServer + real MySQL, end-to-end
  /------------\
 /              \     Unit tests (30+)            — pure business logic, no DB, no server
/----------------\
```

The shape is deliberate: most of the suite is fast, deterministic unit tests against pure
functions (`POSService.calculateSubtotal/calculateTax`, `PayrollService.computeGrossPay/
computeTaxes`, `CartItem` price math, the hand-rolled `Json` reader/writer). Each layer above
that gets progressively slower, more environment-dependent, and more expensive to maintain — so
each layer only exists to catch classes of bug the layer below it structurally cannot:

- **Unit tests** catch arithmetic/logic bugs (wrong tax rate, wrong overtime multiplier, off-by-one
  rounding) in milliseconds, with no setup.
- **Live HTTP integration** (`ApiIntegrationTest`) catches wiring bugs the unit tests can't see —
  routing, JSON (de)serialization at the HTTP boundary, real SQL executing against a real schema.
- **Cucumber BDD** (`ApiCucumberIT`) re-expresses the same API contract in Gherkin, readable by a
  non-Java stakeholder (a PM or QA lead could review `checkout.feature` and know exactly what's
  guaranteed) — it's a documentation and communication layer as much as a test layer.
- **Selenium E2E** (`AdminPanelUiTest`) exists to catch the one class of bug none of the above can:
  the browser actually failing to fetch/render the data (a JS error, a broken fetch call, a CSS
  selector nobody updated). Deliberately kept to two smoke assertions, not a full UI regression
  suite — see "What's *not* tested" below for why.

## What's *not* tested, and why

The Swing GUI layer (`com.possystem.gui.*`, ~17 screens) has **zero automated coverage**. This is
a deliberate scope decision, not an oversight: Swing UI testing tools (AssertJ-Swing, etc.) have a
poor cost/benefit ratio for a single-developer project — they're slow, brittle against layout
changes, and the same business logic they'd indirectly exercise is already covered, directly and
much faster, by the unit tests underneath each screen. If this were a team project with the GUI as
the primary customer-facing surface (rather than the REST API), that calculus would flip and GUI
test automation would move up in priority.

The DAO layer (`com.possystem.dao.*`) is also untested in isolation — it's exercised indirectly
through the live integration/BDD/Selenium tests (which all go through a real MySQL instance), but
there's no DAO-level test-database suite. Noted in the [README roadmap](README.md#roadmap) as a
candidate for a future `@Testcontainers`-based DAO test layer.

## Code coverage (JaCoCo)

`mvn verify` runs `jacoco:check` with a **line-coverage gate scoped to `com.possystem.service.*`,
`CartItem`, and `Json`** — the classes with real, deterministic unit tests — rather than the whole
codebase. Two reasons:

1. Including the GUI/DAO/tools packages would report a misleadingly low number that reflects a
   scope decision (see above), not a quality problem, and would either need an unrealistically low
   threshold to avoid blocking every build, or constant manual overrides.
2. It's **environment-independent by construction**, not just by choice: `ApiServer` runs as a
   separate OS process (`run_api_server.bat`), started outside of Maven, so none of its code —
   including the `POSService.checkout()` family that the live/BDD/Selenium tests exercise over
   HTTP — ever executes inside the JaCoCo-instrumented Maven JVM. Coverage numbers here are
   therefore identical whether or not the API server happens to be running when you run `mvn
   verify`, unlike the test *pass/fail* results themselves.

The gate is set to **10%**, based on a measured baseline of ~12% — not a guess. `POSService` and
`PayrollService` each mix a handful of thoroughly unit-tested pure methods (`calculateTax`,
`computeGrossPay`, ...) with much larger orchestration methods (`checkout`, `checkoutSplitCashCard`,
`runPayroll`) that touch DAOs/the database and only ever execute via the live `ApiServer` process —
never inside the Maven test JVM, so JaCoCo never sees them run. That pulls the bundle-wide ratio
down well below what the pure-logic coverage alone would suggest. The gate sits just under the
measured number so it fails on an actual regression, not on ordinary noise; it's meant to be
ratcheted up as real coverage grows (e.g. once a DAO test layer exists — see Roadmap), not raised
on a guess the way the first version of this gate was (it started at an assumed 30%, which broke
the very first `mvn verify` run against real numbers). Run `mvn test jacoco:report` and open
`target/site/jacoco/index.html` to see the current number.

## Tagging and selective execution

Every test class carries a JUnit 5 `@Tag`: `unit`, `api`, `ui`, or `bdd`, plus `integration` on
the three that need a live server. Surefire and Failsafe both read a `groups` property (empty by
default, so nothing is filtered):

```bash
mvn test -Dgroups=unit          # fast, no server needed — good for a pre-commit hook
mvn verify -Dgroups=unit,api    # unit + live API tests, skip the browser-driven UI test
```

This matters more as a suite grows than it does at 36 tests — but the convention is established
now, while it's cheap, rather than retrofitted later once there are hundreds of tests and no
consistent way to run "just the fast ones."

## Parallel execution — tried, measured, reverted

`src/test/resources/junit-platform.properties` originally enabled **class-level** parallelism
(different classes concurrently, methods within a class sequential — theoretically safe here,
since the four pure unit-test classes share no state and `AdminPanelUiTest`/`ApiIntegrationTest`
keep their own methods ordered). In practice, two consecutive `mvn test` runs against the same
unchanged code printed **different, wrong per-class test counts** in Surefire's console output —
`JsonTest` reported 21 tests instead of its real count, `POSServiceTest` reported 27 then 28,
`PayrollServiceTest` reported 0 then 1 — while the aggregate total (58) and pass/fail counts
stayed correct both times. That's Surefire's plain-text reporter interleaving output across
threads, not actually broken tests, but it means the console output can't be trusted at a glance
under parallel execution in this setup.

At ~58 tests completing in well under a second, the wall-clock win from parallelizing was never
going to be meaningful — so rather than ship confusing console output for a speed gain nobody would
notice, parallel execution is **off by default** (`junit.jupiter.execution.parallel.enabled =
false`). The config is left in place, commented, as a one-line flip if the suite grows large enough
that it starts to matter — at which point it'd be worth also configuring a reporter that handles
concurrent output correctly. Shipping a feature and then measuring it out again, with the evidence
written down, is the point of this section as much as the feature itself.

## CI/CD (`.github/workflows/ci.yml`)

The pipeline spins up a real MySQL 8 service container, loads `schema.sql` + `ci-seed.sql`, starts
`ApiServer` in the background, and runs `mvn verify allure:report` — so every layer, including the
ones that skip locally without a server, executes for real on every push. Surefire (`mvn test`)
and Failsafe (`mvn verify`) are deliberately split: `ApiCucumberIT` lives under Failsafe, not
Surefire, because Cucumber's JUnit Platform engine doesn't translate an `Assume`-based skip thrown
from a `@Before` hook into a real SKIPPED result the way plain JUnit 5 tests do — it surfaces as an
ERROR. Rather than let that intermittently break the default `mvn test` path (which should always
be safe to run with zero setup), the Cucumber suite is isolated to the one command
(`mvn verify`) that's documented as requiring the server to already be running.

## Docker

`docker-compose.yml` brings up MySQL (schema + seed data loaded automatically via
`docker-entrypoint-initdb.d`) and the API server together with one command, for anyone who wants
to try the API without installing Java/Maven/MySQL locally first. It intentionally does **not**
containerize the Swing desktop app — a GUI app needs a display, and forwarding X11 out of a
container is a rabbit hole with a poor payoff for a demo project. `DBConnection` reads its
connection details from environment variables with the original hardcoded values as defaults, so
this required no behavior change for any existing workflow (IDE, `run_*.bat`, CI).

## Roadmap

- `@Testcontainers`-based DAO integration test layer (see "What's not tested" above).
- Cucumber-native tagging (`@smoke`, `@regression` in feature files) for finer-grained BDD
  selection than the current suite-level `@Tag("bdd")`.
- Raise the JaCoCo threshold as coverage grows; consider a second, separate gate once a DAO test
  layer exists.
- Allure trend history across CI runs (currently each run's report is a standalone artifact).
