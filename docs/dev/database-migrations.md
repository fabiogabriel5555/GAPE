# Database Migrations

## Runtime contract

`DatabaseMigrationListener` runs at web application startup. `DatabaseMigrationService` only opens the migration path when `db.bootstrap.mode=none`; schema, demo and full bootstrap modes skip it completely.

Migrations are enabled by default. They can be disabled explicitly with the system property `db.migrations.enabled=false` or environment variable `GAPE_DB_MIGRATIONS_ENABLED=false`.

The runner:

- takes the MySQL advisory lock `gape_schema_migration` before inspecting or changing the schema;
- creates an empty database from `sql/schema.sql`;
- validates an existing unversioned database before recording baseline `V001`;
- applies pending resources in the order declared by `sql/migration/migrations.txt`;
- stores version, description, resource, SHA-256 checksum, duration and status in `gape_schema_migration`;
- verifies all installed checksums on every run;
- validates the required target tables, columns, nullability, unique indexes, triggers and removed artifacts after pending migrations run.

An unversioned database that does not match the reviewed baseline is rejected before a version is recorded. This is intentional: the runner must not claim that an older or partially upgraded schema is current. Structurally compatible legacy artifacts are handled by the ordered migrations below.

Current migrations:

- `V001` validates and records the reviewed structural baseline;
- `V002` removes all legacy student-subject enrollment tables and triggers;
- `V003` converts complete revoked certificate snapshots back to immutable issued certificates, removes revocation columns and restores current constraints; incomplete snapshots or duplicate occurrence/student rows stop for manual resolution;
- `V004` changes class-group code uniqueness from global subject scope to course-occurrence scope;
- `V005` validates registered direct conversations and installs database guards for the exact participant pair, active channel and absence of academic contexts;
- `V006` removes external-delivery metadata and normalizes legacy receipt states to internal delivery only.
- `V013` is a historical migration that introduced a subject initial-course invariant; `V024` removes that obsolete field and invariant, so a subject may now have no course associations.
- `V025` normalizes active associations of inactive subjects into complete historical records and prevents an inactive subject from receiving or retaining an active course association.
- `V014` rejects submitted course-enrollment dates outside their selected occurrence while completing omitted boundaries from that occurrence.
- `V015` requires persisted start and end dates for course, class-group and assessment enrollments, deriving omitted boundaries from their occurrences.

## Adding a migration

1. Add an ordered resource such as `sql/migration/V007__add_example.sql`.
2. Make every statement idempotent because a fresh database is first created from the current `schema.sql` and then processes the manifest.
3. Add `7|Add example|sql/migration/V007__add_example.sql` to `migrations.txt`.
4. Update `schema.sql` to describe the final schema produced by all migrations.
5. Extend the target-structure validation and database tests for the new invariant.
6. Run the focused migration tests against a disposable database, then run the full suite.

Never edit a migration that has been applied. Its checksum is part of the database history. Add a new version for every correction.

MySQL commits many DDL statements implicitly. If a migration fails, the runner records the failed version and stops future startup attempts. Inspect the partial DDL, repair it deliberately, and reconcile the failed history row before retrying. Do not delete or mark a failed row successful without verifying the resulting schema.

## Focused verification

Point the test process at a disposable schema and run:

```powershell
$env:GAPE_DB_URL = "jdbc:mysql://localhost:3306/gape_migration_test?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=LOCAL&forceConnectionTimeZoneToSession=false&preserveInstants=false"
mvn -B -ntp "-Dtest=DatabaseMigrationManifestTest,DatabaseMigrationServiceTest,SchemaIntegrityTest" test
```

The migration tests create and remove schema objects in the configured database. They must not target a database containing data that needs to be preserved.
