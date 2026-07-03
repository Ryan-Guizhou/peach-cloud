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
