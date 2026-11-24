# Peach Cloud Admin Design Context

## Product

Chinese administration console for identities, permissions and system operations. Function Definition maintains the function hierarchy and the resources belonging to the selected function.

## Direction

All application and authentication routes use the approved light console direction: white surfaces, pale blue-gray canvas, dark readable text and blue primary actions. The global semantic variables in `src/style.css` own this palette. Workspace and Profile reuse AdminLayout, including its default responsive behavior. Login, Registration and Password Recovery share AuthShell, the same branding and focus treatment. Workspace shortcuts come from the authorized menu groups; its context is session-backed and it does not display invented monitoring figures.

Menu, Function, Role and Route creation receive their business codes from the backend (M/F/RL/RT with four digits). Editors show automatic generation before save and treat stored codes as read-only. Save payloads omit owned business code fields; function associations, parent links and resource permissions retain their actual backend values. Tables and trees display returned codes without client-side formatting.

Preserve the existing AdminLayout, Ant Design Vue controls and Chinese product terminology. For `/function`, follow the supplied reference: searchable tree on the left, resource filters and a compact table on the right. Highlight the selected node and retain inline node actions. Below 1100px, stack both panels; tree actions remain reachable on narrow screens.

## Runtime tokens

`src/style.css` owns the primitive, semantic and component CSS variables, including the approved global light palette. Ant Design Vue owns control appearance and popup behavior. Feature pages must reuse the global palette, font stack and spacing rhythm.

## Behavior

- Function creation supports roots and children. Codes and application ownership are immutable after creation.
- Tree identity is the function ID; parent lookup uses application plus function code. Preserve ancestor paths while filtering. Surface historical orphan/cycle nodes at the root with a warning.
- Selecting a function resets resource pagination. Cancel previous reads and ignore responses after disposal. Keep failure, loading, no results and no selection distinct.
- Function and resource operations use their existing separate permission prefixes. Hide unauthorized mutations; show a resource permission message when viewing is unavailable.
- Forms use Ant Form validation and modal saving state. Keep dialogs open after failures. Confirm deletion, retain server reference protection, and reload the affected collection after success.

## Verification

Run `npm.cmd run build` and `node --experimental-strip-types --test src/views/system/function/tree.test.mjs`. Verify tree selection, filtering, modal validation, resource CRUD, failures, keyboard access and a narrow viewport in a browser. Backend regression and rollback database tests live in `peach-auth-launch`.

## Login audit and navigation extension

Use backend directory ancestry to group authorised leaves into System Settings, Log Audit, Files and Storage, and Scheduling, retaining Workspace/Profile/Message Center as shortcuts. Directory nodes never become clickable pages. The shared opt-in responsive AdminLayout shell keeps the horizontal navigation and full content width below640px.

The login audit page follows the existing read-only table/card language. Show masked account values; the account filter accepts the full identifier for exact matching. Keep current-tenant and explicitly authorised unassigned-event scopes distinct. Details live in a read-only drawer constrained to the viewport. Cancel stale list and detail requests; provide retry and empty states. No mutation, delete or export actions are present. Logout first reaches the server and keeps the session available for retry on failure.

Additional checks: menu-group unit tests, frontend build, `scripts/login-audit-smoke.py` for browser filter/error/detail/select/narrow/keyboard/logout flows, and the existing function-definition browser regression.

## Role workspace and route list extension

Follow the supplied references: roles, function tree and resource grants in three columns, stacking below 1300px. Keep button/API switches and function checkboxes independent. Search is a view over the complete grant draft; it must never delete hidden selections. Distinguish dirty drafts from saved grants, guard navigation, and require explicit authorization after calibration preview. Default menu choices use saved grants only. Member filtering preserves invisible selections and other roles.

The route page uses module/code/URL filters and columns for module, code, name, URL, component and level, with edit actions. Authentication/cache flags are omitted. Keep modal validation, retry/empty/loading states, request cancellation and viewport-safe forms consistent with Function Definition. Regression: state unit tests, build and `scripts/admin-workspace-smoke.py` against isolated fixtures.

Business navigation comes from the authorized backend menus and routes; an empty snapshot exposes only Workspace and Profile. Known built-in URLs resolve to their canonical local components, so stale backend component metadata cannot turn Login Logs into Workspace. Backend-generated codes do not determine page components or Vue route identity. See [console navigation operations](docs/console-navigation.md) for initialization and verification.

The project has not launched: a fresh empty database schema receives the complete V1 schema, system seed, business codes and permissions. Earlier development V14 databases are not automatically compatible with the new V1; use a new schema or an explicitly backed-up rebuild managed by the user. Do not clean, repair or overwrite a development database as an implicit startup step. Browser sessions must be renewed after switching databases.
