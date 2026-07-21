# GAPE

GAPE is a Jakarta Servlet/JSP academic and pedagogical management application. It is packaged as a WAR for Tomcat 10.1 and uses MySQL 8.

## Requirements

- JDK 25
- Apache Maven 3.9.16 installed and available on PATH
- MySQL 8 for local builds and tests
- PowerShell 5.1+ for the local browser helpers
- A Chromium browser (Brave, Edge or Chrome) for visual validation
- Docker Desktop only when using the container workflow

Maven is required. Install Apache Maven 3.9.16 and restart the terminal before using the project. Confirm the installation with mvn --version.

For a complete Portuguese setup and execution guide, see EXECUCAO.md.
For production TLS, sensitive-data keys and daily backup/restore, see
[`security/https.md`](security/https.md),
[`security/sensitive-data.md`](security/sensitive-data.md) and
[`security/backups.md`](security/backups.md).

## Date and time convention

All dates and times shown to a person use the `Europe/Lisbon` time zone and the
formats `DD-MM-AAAA` and `HH-MM-SS`.

ISO values remain only at technical boundaries that require them: HTML date and
datetime input values, SQL/JDBC values, XML `xs:date` values, API or URL
parameters, migration identifiers, and test instant fixtures. Those values must
be converted to the Lisbon display convention before they reach a user-facing
screen, document, or manual-validation record.

## Build and test

On Windows:

```powershell
mvn test
mvn package
```

On Linux/macOS:

```bash
mvn test
mvn package
```

Database settings can be overridden without editing tracked files:

```powershell
$env:GAPE_DB_URL = "jdbc:mysql://localhost:3306/gape?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=LOCAL&forceConnectionTimeZoneToSession=false&preserveInstants=false"
$env:GAPE_DB_USER = "root"
$env:GAPE_DB_PASSWORD = "root"
```

The WAR is written to `target/gape.war`. Compile/test/package lifecycles remove previous compiled output and test reports, and `package` also replaces the exploded application. A fast build therefore cannot retain deleted classes or mix Surefire results from different runs.

See [`tests/README.md`](tests/README.md) before interpreting focused or historical test reports.

## Schema migrations

When destructive bootstrap is disabled, application startup uses ordered, checksummed migrations and records them in `gape_schema_migration`. Empty databases are created from the canonical schema; existing unversioned databases must match the reviewed baseline or startup fails without recording a version.

See [`dev/database-migrations.md`](dev/database-migrations.md) for the runtime contract, legacy cleanup, failure recovery and the procedure for adding migrations.

## Local Tomcat and browser QA

Build and prepare the validation server:

```powershell
mvn -DskipTests package
.\docs\dev\scripts\browser-prepare.ps1 -SkipPackage
```

The prepare script downloads Apache Tomcat 10.1.24 from the Apache archive on first use, verifies its SHA-512 checksum and installs it under `docs/dev/.tools/`. `mvn clean` therefore does not remove the Tomcat installation.

Run a desktop or mobile smoke check:

```powershell
.\docs\dev\scripts\browser-check-flow.ps1 `
  -Route "/admin/organizations" `
  -ExpectText "Organizations" `
  -Width 390 `
  -Height 844 `
  -NoScreenshot
```

Always stop the validation server when finished:

```powershell
.\docs\dev\scripts\browser-stop.ps1
```

See [`tests/browser-validation.md`](tests/browser-validation.md) for HTTP/resource checks, accessibility checks, screenshots, visual baselines and upload validation.

## Docker (development only)

The Compose stack creates an isolated MySQL service, builds the WAR and exposes GAPE at `http://localhost:8080/GAPE/`. It deliberately runs HTTP and local MySQL without TLS, so it is not a production deployment:

```powershell
docker compose -f docs/dev/docker/compose.yaml up --build
docker compose -f docs/dev/docker/compose.yaml down
```

Set `GAPE_PORT` before starting Compose to use another host port. Use `docker compose down -v` only when the isolated Docker database and upload volumes should be deleted.

## Database CRUD console

The development-only CRUD console is available through Maven:

```powershell
mvn -q -DskipTests compile exec:java
```

It edits the configured database directly and must not be used against production data.

## Project map

- `src/main/java`: controllers, filters, services, DAOs, models and development console
- `src/main/resources`: database configuration, SQL and XML/XSD catalogs
- `src/main/webapp`: JSP/JSPF views and static assets
- `src/test/java`: unit, structural and database integration tests
- `docs/tests`: test procedures and validation evidence
- `docs/dev/scripts`: reproducible browser and local runtime helpers
