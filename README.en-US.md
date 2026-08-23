# peach-cloud

English | [中文](README.md)

Last updated: 2026-08-20
Target stack: JDK 21, Maven 3.9.11, Spring Boot 3.5.4, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0
Project version: `1.0.0-SNAPSHOT`, Maven group: `com.peach`

## Purpose

`peach-cloud` is a Maven multi-module microservice project. The repository contains backend business domains, a gateway, shared components, middleware starters, sample applications, a frontend project, SQL initialization scripts, and a local Docker Compose setup.

This repository is intended to:

- Centralize dependency versions for Spring Boot, Spring Cloud, Spring Cloud Alibaba, Sa-Token, MyBatis, PageHelper, Knife4j, Redis, Redisson, RocketMQ, object storage SDKs, and related libraries.
- Split backend capabilities by domain, including authentication, file service, messaging, settings, monitoring, and code generation.
- Extract reusable capabilities into `peach-component` and `peach-middleware` starter / autoconfigure modules.
- Provide `*-launch` modules for local, Docker, and external configuration-center runtime modes.
- Provide the `peach-cloud-front` frontend project for backend integration.

This repository does not provide:

- A production deployment platform or runtime governance system. CI/CD lives under `deploy-pipline/`; see that README for scope.
- A production-ready guarantee for the local Compose setup.
- Production defaults for local usernames, passwords, ports, or service addresses.
- Complete production semantics for every starter without real external dependencies and business configuration. Messaging, distributed locks, object storage, mail, and cache behavior still depend on the actual middleware and runtime settings.

## Repository Layout

```text
peach-cloud
├── bin/                      # Local Compose helper scripts (default: deploy/docker-compose.yml)
├── deploy/                   # Local Docker layout, Nacos config templates, runtime dirs
├── deploy-pipline/           # GitLab + Jenkins + Registry CI/CD and deploy Compose
├── docs/                     # Project-level design and integration docs
├── doc/                      # Legacy integration notes and component manuals
├── sql/                      # Database initialization and business table scripts
├── peach-auth/               # Authentication, users, roles, resources, login, operation logs
├── peach-gateway/            # Spring Cloud Gateway service
├── peach-fileservice/        # File domain service and file APIs
├── peach-message/            # Site messages, announcements, todos, unread state, WebSocket push
├── peach-setting/            # Dictionaries, value sets, notices, localized messages
├── peach-monitor/            # Monitoring, audit, runtime APIs
├── peach-generator/          # Datasources, metadata, templates, code generation
├── peach-scheduled/          # Scheduler service
├── peach-common/             # Shared constants, responses, exceptions, utilities, base models
├── peach-component/          # captcha, email, initialize, storage, threadpool, scheduler
├── peach-middleware/         # redis, redission, openfeign, satoken, rocket wrappers
├── peach-sample/             # Component and middleware usage samples
├── peach-cloud-front/        # Vue 3 + Vite frontend project
└── pom.xml                   # Maven root aggregator POM
```

Notes:

- `target/`, `.flattened-pom.xml`, logs, and IDE metadata are not source layout.
- `peach-cloud-front` is not part of the Maven reactor. Build it separately with npm.
- Backend runnable applications usually live in each domain's `*-launch` module.

## Module Map

| Module | Submodules / Entry | Responsibility |
| --- | --- | --- |
| `peach-gateway` | `peach-gateway-core`, `peach-gateway-launch` | Gateway startup, routing entry, gateway-side authentication and aggregation |
| `peach-auth` | `common`, `entity`, `service`, `rest`, `external`, `launch` | Users, roles, menus, resources, login, permissions, operation logs |
| `peach-fileservice` | `common`, `entity`, `service`, `rest`, `openfeign-external`, `launch` | File business logic, storage integration, REST APIs, OpenFeign external APIs |
| `peach-message` | `common`, `entity`, `service`, `rest`, `openfeign-external`, `launch` | Site messages, announcements, todos, unread state, push |
| `peach-setting` | `common`, `entity`, `service`, `rest`, `openfeign-external`, `launch` | Dictionaries, value sets, notices, localized messages |
| `peach-monitor` | `common`, `entity`, `service`, `rest`, `openfeign-external`, `launch` | Monitoring, audit, runtime queries, monitor APIs |
| `peach-generator` | `common`, `entity`, `service`, `rest`, `launch` | Datasources, table metadata, templates, preview, code generation |
| `peach-scheduled` | `common`, `entity`, `service`, `rest`, `openfeign-external`, `launch` | Scheduler jobs, execution, OpenFeign external APIs |
| `peach-common` | Single module | Shared responses, exceptions, constants, base models, utilities |
| `peach-component` | `peach-captcha`, `peach-email`, `peach-storage`, `peach-initialize`, `peach-threadpool` | Business-neutral reusable component starters |
| `peach-middleware` | `peach-kafka`, `peach-rocket`, `peach-redis`, `peach-redission`, `peach-mongo`, `peach-satoken`, `peach-openfeign` | Middleware integration, autoconfigure modules, starters, examples |
| `peach-sample` | `SampleApplication` | Local sample application for components and middleware |
| `peach-cloud-front` | `src/`, `vite.config.ts` | Vue 3 + Vite + TypeScript frontend |

## Runtime Entries

| Service | Main class | Configuration directory |
| --- | --- | --- |
| Gateway | `com.peach.gateway.launch.PeachGatewayApplication` | `peach-gateway/peach-gateway-launch/src/main/resources` |
| Auth | `com.peach.auth.launch.PeachAuthServiceApplication` | `peach-auth/peach-auth-launch/src/main/resources` |
| Fileservice | `com.peach.fileservice.launch.PeachFileserviceApplication` | `peach-fileservice/peach-fileservice-launch/src/main/resources` |
| Message | `com.peach.message.launch.PeachMessageApplication` | `peach-message/peach-message-launch/src/main/resources` |
| Setting | `com.peach.setting.launch.PeachSettingApplication` | `peach-setting/peach-setting-launch/src/main/resources` |
| Monitor | `com.peach.monitor.launch.PeachMonitorApplication` | `peach-monitor/peach-monitor-launch/src/main/resources` |
| Generator | `com.peach.generator.launch.PeachGeneratorApplication` | `peach-generator/peach-generator-launch/src/main/resources` |
| Scheduled | `com.peach.scheduled.PeachScheduledApplication` | `peach-scheduled/peach-scheduled-launch/src/main/resources` |
| Sample | `com.peach.sample.SampleApplication` | `peach-sample/src/main/resources` |
| Storage example | `com.peach.example.PeachStoreExampleApplication` | `peach-component/peach-storage/peach-store-example/src/main/resources` |
| RocketMQ example | `com.peach.rocket.example.PeachRocketExampleApplication` | `peach-middleware/peach-rocket/peach-rocket-example/src/main/resources` |

## Stack And Versions

Major versions are managed in `peach-dependencies/pom.xml` and the root `pom.xml`:

| Area | Version |
| --- | --- |
| Java | `21` |
| Maven (recommended) | `3.9.11` |
| Spring Boot | `3.5.4` |
| Spring Cloud | `2025.0.0` |
| Spring Cloud Alibaba | `2025.0.0.0` |
| Sa-Token | `1.44.0` |
| MyBatis Spring Boot Starter | `3.0.4` |
| PageHelper | `2.1.0` |
| Knife4j | `4.5.0` |
| Hutool | `5.8.39` |
| Fastjson | `2.0.58` |
| Redisson | `3.50.0` |
| RocketMQ Spring | `2.3.3` |
| RocketMQ Client | `5.3.2` |
| MinIO Java SDK | `8.5.17` |

Build notes:

- The root POM uses `${revision}` for internal module versions.
- The `development` profile is active by default. Additional profiles are `production`, `docker`, and `test`.
- `maven-compiler-plugin` targets **Java 21** (`maven.compiler.release=21`) with `parameters`.
- Local and CI builds require **JDK 21**. Jenkins uses the Maven 3.9.11 + Temurin 21 image from `deploy-pipline/pipline/maven-node/Dockerfile`.
- `flatten-maven-plugin` generates `.flattened-pom.xml` during builds. Treat it as a build artifact.

## Quick Build

Build all Maven modules and skip tests:

```bash
mvn clean package -DskipTests -Pdevelopment
```

Validate the Maven model and modules:

```bash
mvn clean validate -Pdevelopment
```

Build a business domain and its dependencies:

```bash
mvn -pl peach-auth -am clean package -DskipTests -Pdevelopment
mvn -pl peach-gateway -am clean package -DskipTests -Pdevelopment
```

Build a runnable module and its dependencies:

```bash
mvn -pl peach-auth/peach-auth-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-fileservice/peach-fileservice-launch -am clean package -DskipTests -Pdevelopment
```

Build a component or middleware module:

```bash
mvn -pl peach-component/peach-threadpool -am clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-rocket -am clean package -DskipTests -Pdevelopment
```

## Local Dependencies And Docker Compose

Local Compose lives at `deploy/docker-compose.yml`. Scripts under `bin/` use it to start MySQL, Redis, Nacos, and backend services.

| Service | Container | Local port |
| --- | --- | --- |
| MySQL | `peach-mysql` | `3307 -> 3306` |
| Redis | `peach-redis` | `6380 -> 6379` |
| Nacos | `peach-nacos` | `8849 -> 8848`, `9850 -> 9848` |
| Gateway | `peach-gateway` | `18080` (in-container) |
| Auth | `peach-auth` | `18081` |
| Monitor | `peach-monitor` | `18082` |
| Fileservice | `peach-fileservice` | `18083` |
| Message | `peach-message` | `18084` |
| Setting | `peach-setting` | `18085` |
| Generator | `peach-generator` | `18086` |
| Front | `peach-front` | `80` (mapped by compose) |

Windows:

```bat
bin\start.bat up
bin\start.bat ps
bin\start.bat logs
bin\start.bat down
```

Linux / macOS:

```sh
sh bin/start.sh up
sh bin/start.sh ps
sh bin/start.sh logs
sh bin/start.sh down
```

Supported actions:

| Action | Meaning |
| --- | --- |
| `up` | Runs `docker compose up -d --build` |
| `down` | Stops and removes compose services |
| `restart` | Runs `down`, then `up` |
| `logs` | Follows the latest 200 log lines |
| `ps` | Shows service status |
| `build` | Runs compose build only |

The Windows script accepts a compose file as the second argument:

```bat
bin\start.bat up deploy\docker-compose.yml
```

The Linux / macOS script accepts `COMPOSE_FILE`:

```sh
COMPOSE_FILE=deploy/docker-compose.yml sh bin/start.sh up
```

## CI/CD (GitLab + Jenkins)

Automation lives under `deploy-pipline/` and does **not** modify `deploy/docker-compose.yml`.

| Doc | Purpose |
| --- | --- |
| `deploy-pipline/README.md` | Main CI/CD guide |
| `docs/pipline/peach-cloud-gitlab-jenkins-webhook-guide.md` | Webhook and Jenkins setup |

### CI build environment

| Component | Version |
| --- | --- |
| JDK | Eclipse Temurin **21** |
| Maven | **3.9.11** |
| Node.js | **22** (frontend stage) |

CI image tag: `peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22`

Maven dependencies are proxied through **Nexus 3** on the DevOps stack. Default settings: `deploy-pipline/maven/settings.xml`. See [`deploy-pipline/maven/README.md`](deploy-pipline/maven/README.md).

### First-time DevOps setup

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml config
docker compose -f deploy-pipline/pipline/docker-compose.yml build jenkins
docker compose -f deploy-pipline/pipline/docker-compose.yml up -d
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins docker version
```

Configure Jenkins credentials (`gitlab-peach-cloud`, `peach-deploy-env` Secret file from `deploy-pipline/peach-deploy.env.example`), create a Pipeline job with Script Path `deploy-pipline/Jenkinsfile`, then run **Build Now**.

### Java 21 CI Baseline Check

The CI baseline is Maven 3.9.11 + Eclipse Temurin 21 + Node 22. For first-time deployment or after Jenkins cache changes:

1. Merge the current `Jenkinsfile` to the deploy branch.
2. Optionally rebuild the current CI image locally:

```bash
docker build \
  -f deploy-pipline/pipline/maven-node/Dockerfile \
  -t peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22 \
  deploy-pipline/pipline/maven-node
```

3. Run **Build Now** in Jenkins and confirm `Build CI image` and `Maven package` succeed.
4. Confirm the Maven log uses Java 21 compilation.
5. If dependency resolution fails, clear the Maven cache inside Jenkins and retry.

The CI image must stay aligned with root `pom.xml` (`java.version=21`). Do not fall back to an older JDK baseline image. See `deploy-pipline/README.md` for full details.

## Run A Single Service Locally

To run only one service, start its required dependencies first, then run the corresponding `*-launch` module with Maven.

Examples:

```bash
mvn -pl peach-gateway/peach-gateway-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
mvn -pl peach-auth/peach-auth-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
mvn -pl peach-message/peach-message-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

Notes:

- `application-dev.yml`, `application-docker.yml`, and `application-prod.yml` target local, Docker, and production-like environments.
- Some services rely on external Nacos configuration. Before running a service directly, confirm Nacos, database, Redis, and profile-specific settings.
- Do not commit production secrets, database URLs, object storage keys, or tokens.

## Database Scripts

SQL scripts are under `sql/`:

| File | Purpose |
| --- | --- |
| `INIT.sql` | Initialization entry script |
| `PEACH_USER.sql`, `PEACH_ROLE.sql`, `PEACH_MENU.sql`, `PEACH_RESOURCE.sql` | User, role, menu, and resource tables |
| `PEACH_AUTH_*.sql`, `USER_OPER_LOG.sql` | Authentication, authorization, and operation log tables |
| `PEACH_APPLICATION.sql`, `PEACH_ROUTER.sql`, `PEACH_FUNCTION.sql` | Application, route, and function configuration |
| `PEACH_GENERATOR.sql` | Code generator tables |

Before running scripts, confirm:

- The database character set, collation, and user permissions are suitable.
- The scripts will not overwrite existing production or shared data.
- The local Docker Compose MySQL port is `3307`, not the default `3306`.

## Frontend

The frontend is under `peach-cloud-front/` and uses Vue 3, Vite, TypeScript, Pinia, Vue Router, Ant Design Vue, and Axios.

Install dependencies:

```bash
cd peach-cloud-front
npm install
```

Run locally:

```bash
npm run dev
```

Build:

```bash
npm run build
```

Preview the production build:

```bash
npm run preview
```

Frontend notes:

- `package-lock.json` exists, so npm is the expected package manager for lockfile consistency.
- API base URLs, proxy settings, and authentication integration should be checked with `vite.config.ts` and gateway configuration.
- The frontend is not part of the Maven reactor. A root Maven build does not build frontend assets.

## Configuration Rules

Backend configuration usually comes from three sources:

1. `application.yml` or `application-*.yml` inside each launch module.
2. External configuration such as Nacos.
3. Environment variables from Docker Compose or the runtime platform.

Common profiles:

| Profile | Use case |
| --- | --- |
| `dev` | Local development and direct local dependencies |
| `docker` | Runtime inside Docker Compose networking |
| `prod` | Production or production-like environments |
| `test` | Test environments |

Configuration guidance:

- Start by checking the target service's `*-launch` module and its `application-*.yml`.
- If the service depends on Nacos, confirm Nacos is running and the namespace, group, and data IDs match.
- Middleware starter configuration should follow the module README and configuration classes. Do not infer exact keys only from this root README.

## Component And Middleware Boundaries

`peach-component` contains business-neutral components:

- `peach-captcha`: captcha support.
- `peach-email`: mail sending, templates, retry, routing.
- `peach-storage`: object storage and local / cloud provider integration.
- `peach-initialize`: initialization execution support.
- `peach-threadpool`: thread pools, async execution, context propagation.

`peach-middleware` contains middleware integrations:

- `peach-redis`: Redis utilities, multi-cache, Stream support.
- `peach-redission`: Redisson distributed locks, delay queues, Bloom filters, repeat-prevention.
- `peach-rocket`: RocketMQ production, consumption, event modeling, transactional messaging, examples.
- `peach-mongo`: Mongo autoconfigure and starter modules.
- `peach-satoken`: Sa-Token wrappers for Web and Gateway scenarios.
- `peach-openfeign`: OpenFeign autoconfigure and starter modules.
- `peach-kafka`: Kafka-related module.

Module READMEs should state:

- Which Beans, annotations, template classes, or SPIs a starter provides.
- Which conditions enable autoconfiguration and what default implementation is used.
- How an application integrates the module, overrides default Beans, and extends providers or handlers.
- Production boundaries such as idempotency, transaction semantics, resource auto-creation, path safety, bulk deletion, queue blocking, and thread-pool rejection behavior.

## Documentation Rules

- Chinese docs use `README.md`.
- English docs use `README.en-US.md`.
- Module READMEs should describe real classes, configuration keys, commands, and limitations present in the current source.
- Build artifacts, IDE directories, and logs should not be documented as source layout.
- Do not write production addresses, secrets, tokens, signed URLs, or credentials into README files.
- When a module contains `starter`, `autoconfigure`, and `example` artifacts, document their responsibilities separately.

## Verification

General backend verification:

```bash
mvn clean validate -Pdevelopment
mvn clean package -DskipTests -Pdevelopment
```

Module verification:

```bash
mvn -pl peach-component/peach-storage -am clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-rocket -am clean package -DskipTests -Pdevelopment
```

Frontend verification:

```bash
cd peach-cloud-front
npm run build
```

Docker Compose verification:

```bash
sh bin/start.sh build
sh bin/start.sh up
sh bin/start.sh ps
sh bin/start.sh logs
```

Windows:

```bat
bin\start.bat build
bin\start.bat up
bin\start.bat ps
bin\start.bat logs
```

## Troubleshooting

| Symptom | Check | Resolution |
| --- | --- | --- |
| Maven cannot resolve internal module versions | Whether the command runs from the repository root; whether `${revision}` is active; whether `-am` is missing | Run from the root, add `-am` for selected modules, or run `mvn clean install -DskipTests` first |
| `.flattened-pom.xml` appears after build | The root POM enables `flatten-maven-plugin` | Treat it as a build artifact and do not maintain it manually |
| Service cannot load configuration | Profile, Nacos status, data ID, namespace, group | Check `application-*.yml`, Nacos config, and `spring.profiles.active` |
| Docker Compose port conflict | Local MySQL, Redis, Nacos, or backend service already uses the port | Change compose port mapping or stop the local process |
| Services cannot reach each other | Compose network, service DNS names, container vs host address | Use service names inside Docker networking instead of host `localhost` |
| Database connection fails | MySQL port is `3307`; credentials match; SQL scripts ran | Check compose environment, JDBC URL, and scripts under `sql/` |
| Redis connection fails | Local port is `6380`; password matches the current config | Compare `docker-compose.yml` and service Redis settings |
| Nacos is unavailable | Port `8849`; container health | Run `bin/start.* ps` and `bin/start.* logs` |
| Frontend gets 404 or CORS errors | Gateway status, frontend proxy, backend routes | Check `vite.config.ts`, gateway port `18080`, and service status |
| Starter Bean is not injected | `*-starter` dependency, autoconfigure conditions, enable flags | Check the module README, autoconfigure module, and Spring Boot condition report |

## Maintenance Checklist

When adding or changing a module, check:

- Root `pom.xml` `<modules>` and `<dependencyManagement>`.
- Domain-level `pom.xml` submodule lists.
- Whether `docker-compose.yml` needs a service, port, environment variable, or dependency update.
- Whether `sql/` needs initialization or migration scripts.
- Whether module `README.md` and `README.en-US.md` need updates.
- Whether this root README still has accurate module and runtime-entry information.

## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
