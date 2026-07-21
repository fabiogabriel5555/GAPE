# Test Validation

The Markdown files in this directory document feature-specific coverage and historical executions. Numeric results recorded with a date are snapshots, not proof of the current branch state.

Use focused Maven tests by default. Select the smallest relevant test class or
group from the repository root:

```powershell
mvn -q "-Dtest=TemplateStructureTest" test
```

Before **every** Maven test invocation, focused or complete, stop the GAPE
Tomcat running on 8080 and any other Tomcat using the `gape` database. Tests
must not compete with the live application. After the Maven command finishes,
start/redeploy one clean GAPE instance again on 8080 before any browser or
visual validation.

Run the complete suite only as a final regression gate when the change affects
the application's structure or runtime wiring (for example `web.xml`, filters,
listeners, bootstrap, shared templates or common assets), shared/cross-cutting
backend infrastructure, schema/migrations/seed data, security boundaries, or
when the user explicitly requests all tests/no failures. The scope table in
[`browser-validation.md`](browser-validation.md) is the authority for the
remaining cases:

```powershell
mvn -q test
```

Database tests recreate the schema configured by `GAPE_DB_URL`. Point that variable at a disposable MySQL 8 schema or use the CI service database; never run the full suite against data that must be preserved.

The Maven lifecycle removes `target/surefire-reports` and compiled output before each run. Consequently, reports under `target/surefire-reports` describe only the latest Maven invocation. A focused invocation contains only those selected suites and must not be presented as a full regression result.

The GitHub Actions workflow runs `verify` on a clean checkout with an isolated MySQL service and publishes the WAR only after the complete suite passes.

For frontend validation, follow [`browser-validation.md`](browser-validation.md). It defines the evidence-first fast single-server loop, exact-state viewport/geometry checks, network and resource checks, basic accessibility checks, fixture protection and optional visual baselines. For interaction changes it also requires a complete replay with real visible clicks/selections, intermediate-state evidence, final-state screenshots and a cleanup proof. A generic page render, direct request or hidden-input assertion is not sufficient evidence for a defect that occurs in a particular tab, modal, expanded row or selection state.

The real-address rule is strict: acceptance evidence must be collected from
`http://localhost:8080/GAPE/`, never from port 18080 or an isolated Browser
Tomcat. For a list/archive/filter change, prove both sides of the result in
one browser session: click to open the relevant view, verify the complete
primary list and its count, verify the complete secondary/archive list and its
count, and use scoped selectors to prove that excluded states are not still
duplicated in the wrong container. Then reload and repeat the final checks at
desktop and mobile sizes. A screenshot of only the archive, a route smoke
check, or a global text match is insufficient proof.

## Rigor required for complete-flow validation

When a task changes an interaction, validating only that the route renders or
that a modal opens is not a complete test. The entire journey must be replayed
from the user's initial state to the persisted final state with the real
controls: open the route/card/tab, click the action, make every required
selection, submit, wait for the operation to settle, verify the resulting
screen and reload once to confirm persistence. The same journey must cover the
invalid/disabled state when the task defines one (for example, submit before a
required selection and after clearing a dependency).

Each journey requires proof of the initial state, every meaningful intermediate
state and the final state. Use screenshots of the disputed visual state and
record the click trace, final URL/card/tab, response status, spinner transition,
success/error message, created/updated identifier and the post-reload result.
The browser log must show zero unexpected network, console, navigation,
resource or accessibility errors. A connection-refused page, CSP/frame error,
unsettled spinner, wrong redirect, missing row, or a screenshot captured before
the UI settles is a failed validation, never a pass.

DOM/HTTP assertions may measure or verify the result, but they cannot replace
the visible click, selection or submit that caused it. If the integrated browser
is unavailable, use the documented Brave/CDP fallback and record that fact;
never downgrade the journey to a direct request. Any unexercised branch must be
reported as `unverified/blocked`, not described as fully validated.

All browser tests and visual validations must run against the real application
address used by the user. In the current project that address is
`http://localhost:8080/GAPE/`; port 18080 and the disposable Browser Tomcat are
not valid test targets and must not be used as substitutes.

Before **each** test/visual-validation batch, identify and stop the Tomcat
process serving GAPE on port 8080 (and any other Tomcat using the `gape`
database), then start one clean deployment again on port 8080. Run every
route, click flow, selector assertion, screenshot, desktop/mobile check and
cleanup against `http://localhost:8080/GAPE/`. If the real 8080 deployment
cannot be started, mark the batch unverified/blocked; do not move it to 18080.

At the end of **every** test or visual-validation batch, stop the validation
Tomcat and verify that port 8080 is completely free. A batch is not complete
while `Get-NetTCPConnection -LocalPort 8080 -State Listen` returns any
listener; identify and stop the exact owning process, then repeat the check.
Do not leave the clean deployment running for the next task and do not count a
still-listening user/IDE Tomcat as a valid cleanup result.

For a narrow frontend correction, run focused tests and prove the exact visual
state before deciding whether a final full regression gate is warranted. Do not
use the full suite as a way to diagnose layout, and do not run it after every
small edit or repeatedly without a code/test-state change. Batch related edits
and run one final suite at most. Before that suite, stop **every Tomcat process
whose deployed GAPE application uses the `gape` database**, including the user
Tomcat on 8080, the Browser Tomcat and any IntelliJ/IDEA Tomcat on another
port. A port-only check is insufficient: inspect each process' `catalina.base`
and deployed GAPE database configuration. Record the visual fixture, and
document how it will be restored if the suite changes the database.

Because the complete suite is the slowest validation in the project, run it
under controlled conditions: use a disposable MySQL schema
through `GAPE_DB_URL`, close/stop all competing web servers and test runners,
run one invocation only after focused tests and visual acceptance, capture the
elapsed time and exit code, and do not repeat it merely to obtain a cleaner
log. If a run reports a database lock/deadlock or another process collision,
the run is invalid as a regression result; fix the environment, repeat once,
and record both the invalid run and the clean run.

## Date and time convention

Acceptance tests and visual checks must treat `DD-MM-AAAA` and `HH-MM-SS` in
`Europe/Lisbon` as the user-facing contract. ISO literals in SQL, JDBC, XML,
HTML input `value` attributes, request parameters, migrations, and fixed
`Instant` fixtures are technical representations; they are not valid display
evidence until converted at the UI or document boundary.
