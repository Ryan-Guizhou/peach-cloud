# Starter Configuration Metadata Maintenance

English | [中文](starter-configuration-metadata.md)

## Intent

After adding a `peach-*-starter`, Spring Boot-aware editors should offer discoverable configuration property names, types, descriptions and statically known defaults. Metadata does **not** establish that a required runtime Bean or external service is available.

## Contract

1. Every `*-autoconfigure` module under `peach-component` and `peach-middleware` contains `src/main/resources/META-INF/additional-spring-configuration-metadata.json`, available transitively through its Starter. Empty `properties` and `hints` arrays are allowed only where no public settings exist.
2. Modules declaring `@ConfigurationProperties` depend on `spring-boot-configuration-processor` with `optional=true`. Let the annotation processor generate `META-INF/spring-configuration-metadata.json`; do not commit its compiled output. Additional metadata fills in descriptions, dynamic-map hints and defaults that need manual help.
3. All metadata `properties[].description` values must be written in Chinese with a meaningful explanation of the setting; hint descriptions must also use Chinese. English placeholders are rejected by CI. Every added property must have an additional metadata entry. Defaults must agree with literal Java initializers where statically inferable. Never fabricate defaults for credentials, endpoints or runtime-computed values; explain external configuration and conditional defaults in descriptions.
4. Use canonical kebab-case paths; nested data uses dotted paths and dynamic map keys use `*` (e.g. `peach.storage.providers.*.endpoint`). Do not define contradictory defaults in multiple libraries.
5. Include public `@ConditionalOnProperty` and `@Value` switches not represented by a configuration-properties class manually. An empty document is valid for a genuinely parameter-free Starter.

## CI Gate

```bash
node scripts/check-spring-configuration-metadata.mjs
node scripts/verify-changes.mjs
```

The existing repository gate checks every AutoConfigure metadata file, JSON syntax, uniqueness, names, types, Chinese descriptions, Java field coverage (including nested types and same-package config objects), annotation processor dependencies and statically inferable default-value drift.

For initial scaffolding:

```bash
node scripts/check-spring-configuration-metadata.mjs --fix
```

The `--fix` mode generates placeholders for descriptions; **review them** before committing. It cannot safely infer all constant-based defaults, external Nacos overrides, dynamic maps, or Bean-dependent runtime defaults.

## Acceptance

The standard Maven reactor must pass, with generated and additional metadata packaged under `META-INF` of the AutoConfigure JAR. Validate actual completion in a Spring Boot-aware IDE using a dependent Starter Quickstart or service. Editor indexing and cache freshness are external to the JSON contract.
