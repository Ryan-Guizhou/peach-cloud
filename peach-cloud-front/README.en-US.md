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

### Task scheduling

- After login, the Task scheduling navigation exposes job management, execution records and registered Handlers. Scheduler entries and buttons currently have no permission-code filtering; other modules retain their existing permission behavior.
- Pages use the shared Axios client and `/api/scheduler/**` with the default base URL. The gateway must forward these requests to `/scheduler/**` on `peach-scheduler`. The development proxy continues to use the gateway address in `vite.config.ts`.
- Saving requires an online Handler; new jobs start as drafts. Action availability follows backend states. Manual retry and cancellation require a reason.
- Backend pages return arrays without a total count, so jobs and executions use previous/next navigation. A full page can be followed by an empty page.
- Full integration requires the gateway, authentication and scheduler services, their MySQL, Redis, Nacos and RocketMQ dependencies, and a business service registering a Handler. A successful frontend build does not establish end-to-end execution verification.

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.

### Scheduler service demos and filtering

Handler management retains online and offline records and displays only heartbeat status, with server filtering by application, keyword and online status. There is no Handler enabled switch or schema migration requirement. Task lifecycle enable/disable remains available. Execution cards filter by execution ID, job code, state, trigger source, executor and scheduled time range.

Launch auth and setting from their existing application entry points in IDEA with the normal `dev` (or `docker`) profile. Local application YAML retains startup and Nacos imports; Nacos manages scheduler, virtual-thread, Outbox and RocketMQ settings. Services automatically register `authSchedulerDemo` and `settingSchedulerDemo` and share the same infrastructure environment with the control plane. The demo handlers return success without modifying business data.

Once both Handlers appear online, inspect their automatically registered tasks: auth uses a Cron schedule every minute; setting uses a fixed interval of 60 seconds. Review the centrally configured initial state before testing. Real execution verification requires live services; compilation and simulated responses do not establish successful registration or execution.


## Automatic task registration

Services declare stable `jobCode` and `jobName` in `@PeachJob` and supply complete defaults through Nacos `peach.scheduler.executor.jobs[handlerName]`: schedule, time zone, misfire and concurrency policies, timeout, retries, parameters and initial state. The control plane validates the definition on first registration and creates the task. Missing metadata permits Handler registration and heartbeats only, without creating a placeholder draft. Later heartbeats preserve schedules, parameters and lifecycle or deleted states maintained by administrators. Additional tasks may use the same Handler. Initial state is managed centrally.

## Function Definition

`/function` presents a function tree beside a resource table. It supports hierarchy maintenance and filtering, pagination and CRUD for resources of the selected function. Function codes are immutable after creation; children or references prevent deletion. Resource actions retain the `resource:*` permissions.

Existing environments must run the [Function Definition migration](../sql/migrations/20261122_function_definition.sql), then sign in again to refresh menu and gateway permission snapshots. Tree regression tests: `node --experimental-strip-types --test src/views/system/function/tree.test.mjs` (Node.js 24).

Browser regression: build first, then run `python scripts/function-definition-smoke.py`. Requires Chrome and Python `websocket-client`. The script uses local API fixtures only and checks tree selection, filtering/error recovery, form validation, resource CRUD and narrow layouts.

## Login logs and navigation

`/login-log` supports time, exact full-account, result, event and IP filters, plus read-only details. Unassigned events require platform audit permission. The sidebar follows backend directories; explicit logout calls the server first. See [Login audit](../peach-auth/docs/login-audit.en-US.md) for upgrade and backend boundaries. Browser regression: `python scripts/login-audit-smoke.py` after building (requires Chrome and Python `websocket-client`).

## Role workspace and route list

The role list, function tree and resource grants use three columns. Filtering preserves hidden grants; calibration previews changes before explicit save. Membership and default menus have separate editors. Routes combine module code, route code and URL filters, with authentication/cache columns hidden. See [administration workspace](../peach-auth/docs/admin-workspace.en-US.md) for APIs and migration. After building, run `python scripts/admin-workspace-smoke.py` for browser regression against isolated API fixtures.
