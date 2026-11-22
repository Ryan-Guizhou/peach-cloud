# Role workspace, routes and operation logs

[中文](admin-workspace.md)

## Roles and routes

The role screen contains the role list, independent function checkboxes and resource switches. Role keywords search code or name. Route filters combine module code, route code and URL substring matching. The server binds queries to the current tenant, plus organization/year for roles and application for routes. Route authentication/cache flags are hidden and preserved when omitted. Editing updates the existing row; route codes are unique within tenant/application, and component choices come from existing frontend views.

Filtering retains ancestor paths and grants outside the visible results. Authorize saves the complete function/resource draft atomically. Resources must belong to selected functions. Existing invalid grants remain visible until explicitly removed. Calibration previews invalid, disabled or out-of-selection grants; confirming changes only the draft, and Authorize persists it. Calibration never adds grants.

Membership updates replace only this role's associations and preserve other roles. Default menus must come from this role's saved authorized menus. Empty resets to Workspace. Only single-role login uses the preference; multi-role login still opens Workspace. Explicit safe return paths and normal route permission checks remain effective. Default-menu changes take effect on the next login.

New workspace writes use READ_COMMITTED transactions and a role row lock; grant saves also lock selected functions. Grant and membership revisions reject stale snapshots. Function/resource grants commit together. Permission caches refresh only after commit. Role codes are immutable, and referenced roles cannot be deleted.

## Workspace endpoints

All paths below use `/auth/role`. GET requests take `roleId`; POST requests take `RoleWorkspaceDTO` with required `roleId`, `revision` and complete relevant collections.

| Method and path | Data | Business permission |
| --- | --- | --- |
| GET `/workspace` | Role, definitions, grants, menu choices, revision | `role:view` |
| GET `/members` | User choices, current members, membership revision | `authorization:update` |
| POST `/saveGrants` | `funcCodes`, `resourceKeys` | `authorization:update` |
| POST `/calibrate` | Same input, removal preview only | `authorization:update` |
| POST `/saveMembers` | `userCodes`, membership revision | `authorization:update` |
| POST `/saveDefault` | `skipUrl`, grant revision | `role:update` |

Resource keys concatenate length-prefixed function code, type and resource code. Implementations are `RoleWorkspaceService.key` and `views/system/role/state.ts`. Gateway API authorization still applies; the service additionally checks the current user's corresponding button permission.

## Migration and activation

Existing databases upgrade through startup migrations; `sql/migrations/20261122_role_route_workspace.sql` remains a historical archive and fresh-install SQL is synchronized. The migration expands `PEACH_ROLE.SKIP_URL` to 100 characters, registers six APIs and derives API grants from corresponding existing button grants without adding button permissions.

Back up the database before deployment, then let shared Flyway migrations upgrade it at application startup; see [Database migrations](../../peach-database/README.en-US.md). Temporary Python tools have been removed. MySQL DDL can commit implicitly: inspect actual schema and migration history after failure; no automatic repair or database cleanup is performed.

Build and restart auth, then sign in again to load the endpoints, collector and permission snapshot. Editing source does not add new beans to an existing IDE process.

## Operation logs

The aspect under `com.peach.log` captures annotated operations on success, business failure and thrown exceptions without replacing the original result or exception. Tenant, organization and actor are captured from trusted request-thread security context. Missing trusted scope prevents unowned inserts. Mapping `USER_OPER_LOG.TENANT_ID/ORG_ID` in the model and SQL fixes inserts rejected by the existing required columns.

The default consumer interval is 5000ms, configurable with `peach.operation-log.flush-interval-millis`, and each batch contains at most 200 records. Failed batches remain pending and retry with stable IDs; duplicate inserts are idempotent by primary key. Independent batch commits allow failed operations to survive business rollback. Queries are scoped to current tenant/organization.

Collection keeps static operation labels, allowed target identifiers, outcome, duration, method and request path. Legacy DTO-interpolating SpEL is ignored. Full requests, responses and raw exception messages are excluded. Target fields are roleId, roleCode, routerId, routerCode, userId, funcId, funcCode and resourceId, limited to letters, digits, underscores and hyphens. IP fields use the direct peer address, without trusting forwarded headers.

The queue and pending batch remain in memory: a process crash can lose uncommitted records, and persistent database failures block the pending batch. This is not a durable Outbox. Only annotated operations are collected. System logs and Loki integration are outside this change.

## Verification entry points

`OperationLogRegressionTest` covers exception collection and batch retry. `RoleWorkspaceSecurityTest` covers permissions, scope and default landing behavior. With `PEACH_ADMIN_DB_SMOKE=true`, `AdminWorkspaceDatabaseTest` verifies combined queries, updates, grants, membership isolation and success/failure records through aspect, queue and actual MySQL, including duplicate delivery. Isolated test data is cleaned up.

Build with `npm.cmd --prefix peach-cloud-front run build`. `scripts/admin-workspace-smoke.py` tests the built frontend against isolated API fixtures for filter-safe grants, calibration, membership, default menus, route editing and narrow layouts. It does not replace an end-to-end check against the currently running service.
