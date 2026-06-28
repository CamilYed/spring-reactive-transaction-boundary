# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [0.1.0] - Unreleased

### Added

- Added Spring-independent `ReactiveTransaction` API for application services.
- Added `Mono` transaction boundary support through `inTransaction(...)`.
- Added `Flux` transaction boundary support through `inTransactionMany(...)`.
- Added `TransactionOptions` for propagation, isolation, read-only mode, and timeout.
- Added Spring adapter backed by Spring's `ReactiveTransactionManager`.
- Added Spring Boot 4 auto-configuration.
- Added Spring Boot starter.
- Added PostgreSQL/R2DBC integration tests with Testcontainers.
- Added PostgreSQL verification for commit, rollback, read-only transactions, isolation, propagation, and timeout behavior.
- Added standalone Spring Boot WebFlux/R2DBC/PostgreSQL DDD demo application.
- Added SonarCloud, JaCoCo, Spotless, and GitHub Actions CI verification.
- Added signed release bundle workflow for Maven Central preparation.

### Verified

- Verified JDK 21 support.
- Verified Spring Boot 4.x usage.
- Verified PostgreSQL as the first tested R2DBC database target.
- Verified transaction commit and rollback behavior for `Mono` and `Flux`.
- Verified propagation, isolation, read-only, and timeout option mapping.

### Notes

- This is the first public release candidate.
- The API should be considered pre-`1.0.0`; breaking changes may still happen before `1.0.0`.
