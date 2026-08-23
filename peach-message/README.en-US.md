# peach-message

English | [中文](README.md)

## Purpose

`peach-message` is the user-facing message center. It manages site messages, announcements, todos, multi-type publishing, unread state, message recall, and realtime push. It is not an MQ starter; asynchronous event transport should be handled by middleware such as `peach-rocket`.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-message-common` | Message-domain constants, enums, and shared objects |
| `peach-message-entity` | Message DO/DTO/QO/VO models |
| `peach-message-service` | Publishing, querying, read-state updates, recall, WebSocket, and Redis Pub/Sub |
| `peach-message-rest` | Internal and external REST APIs |
| `peach-message-openfeign-external` | External OpenFeign client |
| `peach-message-launch` | Runtime application module |

## Core Capabilities

- Publish message, announcement, todo, and other message types.
- Query unread count, message list, and message detail.
- Mark one message or all messages as read.
- Recall messages by source type and source code.
- Push messages to online users through WebSocket.
- Distribute WebSocket push events across nodes through Redis Pub/Sub.

## Key Entrypoints

- Application: `peach-message-launch/src/main/java/com/peach/message/launch`
- Service layer: `peach-message-service/src/main/java/com/peach/message/service`
- REST controllers: `peach-message-rest/src/main/java/com/peach/message/rest`
- WebSocket: `peach-message-service/src/main/java/com/peach/message/websocket`

## Relationship with RocketMQ

- `peach-message` owns user-message persistence, querying, and notification delivery.
- `peach-rocket` owns async domain-event transport and reliable delivery.
- Recommended flow: business services publish domain events; `peach-message` consumes them and converts them into user messages.

## Boundaries

- The current realtime push path is Redis Pub/Sub + WebSocket.
- Final consistency, duplicate consumption, and compensation must be designed with the upstream event flow.
- WebSocket authentication, online state, and multi-device policies should be extended per product requirements.

## Verification

```bash
mvn -f "peach-message/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
