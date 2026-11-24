# Peach Database

[Chinese](README.md)

Shared MySQL initialization for the JDBC applications. This pre-release repository now has one complete Flyway V1: it directly executes the packaged `ALL_TABLE_CREATE.sql` and `INIT.sql`. It does not replay an internal history of upgrades.

## Starting a development database

Use an empty development schema and start an application with its existing data source configuration. V1 creates the complete current schema, 27 page routes, menus, function relationships, administrator grants and four-digit M/F/RT/RL codes. There are 255 explicit console resource definitions plus the retained platform-login-log and router-delete permissions (257 total). Resource permission identifiers remain unchanged. The four code rules and durable issuance ledger are initialized; there is no historical code mapping table in a new installation.

The seed is derived exclusively from an isolated empty database initialized by repository-owned system scripts. It contains no copied business database rows. Existing development default-account credentials are preserved by the seed contract; change them before exposing the environment. No credential value is documented here.

## Existing local databases

The consolidated V1 is incompatible with old development histories including V14. Do not clean, repair, delete history, or rebaseline an existing database automatically. Back up any useful local data and configure a new empty schema, then re-enter only reviewed development data. Unversioned nonempty schemas and old BASELINE/applied-version histories are explicitly rejected before migration. The existing local database is not changed by repository edits.

A MySQL named lock serializes initialization across application instances. Hikari maximumPoolSize must be at least 3. Failures block startup. DDL may commit independently, so a failed first initialization can leave a partial schema; inspect it and use a fresh empty development schema. Automatic clean and repair are disabled. Successful repeated starts validate checksums without overwriting user edits.

## Maintaining the first version

Edit individual table files under `sql/` and the system seed `sql/INIT.sql`, then run from the repository root:

```bash
node scripts/sync-database-sql.mjs
node scripts/verify-changes.mjs
```

The generator synchronizes the combined schema and Docker initialization mirrors. Historical manual scripts under `sql/migrations/` are reference material, not startup instructions. Runtime migration scanning contains no V2-V15 chain. After the first actual release, freeze V1 and add forward migrations for subsequent changes.

Verification uses JDK 21 and isolated MySQL schemas: complete initialization, valid references and API grants, four-digit allocation and exhaustion, repeated/concurrent startup, checksum/SQL failure protection, legacy-history rejection and Boot startup wiring. Integration fixtures remove only their generated test schemas.
