# Flyway startup synchronization implementation plan

> Execute task by task with Java engineering and verification skills. The user confirmed the shared migration/all-pending-changes contract.

**Goal:** JDBC application startup synchronizes the shared MySQL schema and system seed with Flyway before application data access.

**Architecture:** Application-specific `peach-database` module packages the existing baseline SQL and versioned historical patches. Spring Boot initializes Flyway with a shared history table and guarded baseline version 0. New databases and recognized existing Peach databases follow the same forward migrations; non-Peach databases are rejected.

**Constraints:** No clean, automatic repair, credential logging, user-password policy change or destructive schema reset. Preserve existing data and history. MySQL DDL is not transactionally reversible. Never push directly to main.

- [x] Add failing MySQL integration coverage for empty databases, legacy schemas, repeated/concurrent startup, non-Peach refusal and checksum/SQL failures.
- [x] Add `peach-database` dependencies, guarded strategy and checksum-bound baseline migrations; freeze historical patches under Flyway names and remove embedded transaction statements.
- [x] Wire all seven JDBC launch modules. Run real MySQL integration and startup ordering tests.
- [x] Audit pending files; remove unreferenced local artifacts, replace Python migration entrypoints, retain used SQL mirrors with generator/check.
- [x] Synchronize bilingual docs, frontend CI and migration CI.
- [ ] Verify affected Maven/npm/build/browser/repository checks, stage batches by feature, push branch, open PR.
- [ ] Wait for every CI check on the latest PR head; resolve failures; merge only after all pass and update local main without losing changes.
