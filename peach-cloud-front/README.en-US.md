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
