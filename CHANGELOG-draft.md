# Changelog

All notable changes to this project will be documented in this file.

This project follows semantic versioning. Before `1.0.0`, the public API may still evolve.

## [Unreleased]

### Added

- Spring-independent `ReactiveTransaction` public API for application-layer transaction boundaries.
- Dedicated transaction methods for `Mono` and `Flux` workflows.
- `TransactionOptions` for propagation, isolation, read-only mode, and timeout configuration.
- Spring adapter backed by Spring reactive transaction infrastructure.
- Spring Boot 4 auto-configuration module.
- Spring Boot starter module.
- Auto-configuration backoff when an application provides its own `ReactiveTransaction` bean.
- PostgreSQL/R2DBC integration tests with Testcontainers.
- PostgreSQL verification for commit, rollback, read-only mode, isolation, propagation, diagnostics, and timeout behavior when PostgreSQL `statement_timeout` is applied on the transactional connection.
- Unit tests for API defaults and API-to-Spring transaction option mapping.
- Standalone Spring Boot WebFlux/R2DBC/PostgreSQL DDD demo application.
- CI verification for the root project and standalone demo application.
- Spotless formatting and optional Git hooks.

### Changed

- Migrated package namespace to `io.github.camilyed`.
- Improved README structure, installation instructions, verification matrix, and demo navigation.
- Clarified that PostgreSQL is the first verified database target.
- Clarified that the public API module remains Spring-independent and database-independent.

### Notes

- Current development version is `0.1.0-SNAPSHOT`.
- Snapshots are published to the Sonatype Central Portal snapshots repository.
- First public release target: `0.1.0`.

## [0.1.0] - TBD

Initial public release.

### Added

- Spring-independent reactive transaction boundary API.
- Spring adapter.
- Spring Boot starter and auto-configuration.
- PostgreSQL/R2DBC verification with Testcontainers.
- WebFlux/R2DBC DDD demo application.
