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
