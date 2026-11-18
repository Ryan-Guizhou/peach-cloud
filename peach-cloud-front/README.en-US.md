# peach-cloud-front

English | [中文](README.md)

## Purpose

`peach-cloud-front` is the frontend project directory for Peach Cloud. The backend repository is primarily organized around Java services, so frontend build commands must follow the actual `package.json` and framework configuration inside this directory.

## Usage Rules

- Do not assume the frontend framework, routes, or proxy rules from backend documentation; use files inside `peach-cloud-front` as the source of truth.
- Frontend-backend integration should go through `peach-gateway` when possible.
- Environment variables, API endpoints, and auth tokens must not hard-code production values.

## Verification

If `package.json` exists, run the configured scripts:

```bash
npm install
npm run dev
npm run build
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.

### SaaS IAM frontend safety baseline (#210, incremental)

Business routes are built **only from the Auth-provided routerList**; an empty permission/router response no longer activates locally predefined administrator routes. Only server-authorized menu items are displayed. When none are available, a protected `/no-access` page is shown instead of inventing admin access. The default login redirect lands there if no `/workspace` route was granted. Both workspace and admin-layout logout attempt `POST /auth/logout` before always clearing local tokens, dynamic routes and WebSocket connections in `finally`, even if the network fails. The `year/tenantRefId/memberRefId/appRefId` login contract is now implemented, including post-password tenant/application selection, explicit calendar-year choice, members without a selected organization, and second-step submission. Persisted sessions reject legacy `fiscal` and incomplete physical identity. The header can switch authenticated tenant/application/year, clearing stale routes and WebSocket subscriptions. If Auth has switched the token context but the frontend fails to synchronize its state, the client drops the local session and requires a new login rather than retaining a stale tenant view. **Organization, membership, role and row/field policy administration have not yet been fully migrated, and Auth may still return an empty authorization snapshot. Keep the PR in Draft; Vue build success alone does not complete #210 acceptance.**

A server-provided menu without a matching authorized router entry is not displayed through a local component fallback. The current member's `routerList` is the only source of navigable business routes. `fallbackAdminRoutes` is solely a build-time component mapping for an already authorized route, never an independent permission grant.

### Typed SaaS IAM management writes (#210, incremental)

New `src/api/saas-iam.ts` provides typed, protected calls corresponding to the existing `SaasRoleController` and `SaasPolicyController`: create a tenant/year role, replace permission assignments, grant/revoke one scoped member-role binding, and replace data-row and field read/write policies. It accepts only physical 32-character UUIDs selected from a trusted server directory and operation data, rejecting legacy `tenantId/orgId/fiscal/ROLE_CODE` joins. Scope combinations, role/resource code syntax and batch caps are checked before requests, but **Auth remains the authority for user identity, tenant, year and every permission decision**. Safe paginated member/role/permission directory endpoints are not yet available, so the UI must not ask administrators to guess or manually enter physical foreign keys. The management forms have not yet been fully migrated; keep this PR in Draft until directory and workflow integration are complete.

### SaaS role administration UI (#210, incremental)

Legacy frontend mutation contracts such as `/auth/role/add` and `/auth/authFunction/saveRoleFunctions`, which combine `tenantId/orgId/fiscal` and role business codes, have been removed from `api/admin.ts`. The role screen now uses `createTenantRole()` within the verified token's server-side tenant/application/year, with generated physical IDs returned read-only and never manually entered. The authorization screen no longer submits legacy cross-domain writes; it awaits secure paginated role, member and delegable-permission directories before wiring the typed `saas-iam.ts` operations. **This is an explicit safety boundary, not completion of the full management UI or Issue #210.**

### #227 Functional SaaS role administration

`src/api/saas-iam.ts` now provides protected `fetchSaasCatalog`, `fetchRolePermissionRefs` and `fetchMemberBindings` calls. The yearly role administration page uses bounded server-side role, delegable permission, member and organization selectors to replace a role's full permission set, grant scoped `TENANT/ORG/ORG_TREE` member-role bindings and revoke a **specific physical binding**. Full replacement is disabled when the delegable directory has more pages or the role holds permissions not visible to the current administrator, preventing accidental privilege removal. The browser cannot edit `tenantRefId/appRefId/year`. Live database, gateway IAM and historical data migration acceptance remain outstanding. Data/field policy editing UI needs a further trusted field-definition directory.

**Authorization-version consistency:** Replacing a role's full permission set invalidates the tenant-wide `AUTH_VERSION`, including the acting administrator. Following a successful write, the UI disconnects its old WebSocket, clears the local token and dynamic routes, and requires a fresh login instead of fetching with a now-revoked token and misreporting a successful update as failure. Tenant/application/year switches also clear old directory selections and cached catalog entries to avoid stale cross-tenant UI state.

### #229 Protected row/field policy editor

New `src/views/system/authorization/SaasPolicyEditor.vue` selects an action from the role's actual granted permissions, then loads the **complete stored data-scope rules and field read/write rules** before permitting any full replacement. Field definitions must come from a trusted tenant/application field directory, with bounded pagination. Full replacement remains disabled when the directory is incomplete, an existing field is not visible, the organization directory is incomplete, or policies are duplicated/invalid. Data scope supports SELF/ORG/ORG_TREE/TENANT_ALL; field reads allow ALLOW/MASK/DENY and writes ALLOW/DENY. Physical IDs are chosen only from trusted selectors. Successful changes invalidate the entire tenant's authorization version, so the client immediately discards its old session and asks for a fresh login. This is policy configuration, not the downstream database-level SELECT/COUNT/EXPORT/PATCH enforcement.
