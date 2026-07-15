# Test Validation

The Markdown files in this directory document feature-specific coverage and historical executions. Numeric results recorded with a date are snapshots, not proof of the current branch state.

Use the locally installed Maven from the repository root for current evidence:

```powershell
mvn test
```

Database tests recreate the schema configured by `GAPE_DB_URL`. Point that variable at a disposable MySQL 8 schema or use the CI service database; never run the full suite against data that must be preserved.

The Maven lifecycle removes `target/surefire-reports` and compiled output before each run. Consequently, reports under `target/surefire-reports` describe only the latest Maven invocation. A focused invocation contains only those selected suites and must not be presented as a full regression result.

The GitHub Actions workflow runs `verify` on a clean checkout with an isolated MySQL service and publishes the WAR only after the complete suite passes.

For frontend validation, follow [`browser-validation.md`](browser-validation.md). It defines the fast single-server loop, network and resource checks, basic accessibility checks, viewport evidence and optional visual baselines.

## Date and time convention

Acceptance tests and visual checks must treat `DD-MM-AAAA` and `HH-MM-SS` in
`Europe/Lisbon` as the user-facing contract. ISO literals in SQL, JDBC, XML,
HTML input `value` attributes, request parameters, migrations, and fixed
`Instant` fixtures are technical representations; they are not valid display
evidence until converted at the UI or document boundary.
