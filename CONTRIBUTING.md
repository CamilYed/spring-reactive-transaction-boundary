# Contributing

Thank you for your interest in contributing to Spring Reactive Transaction Boundary.

This project is in early development, so APIs may still change before the first stable release.

## Development requirements

* Java 21
* Gradle Wrapper
* Git

## Before opening a pull request

Run the full verification locally:

```bash
./gradlew spotlessCheck clean build
```

If formatting fails, run:

```bash
./gradlew spotlessApply
```

and commit the formatting changes.

## Coding style

* Public API JavaDoc must be written in English.
* Public exception messages must be written in English.
* Public API should avoid `null` whenever possible.
* Public API should remain small and explicit.
* Spring-specific types must not leak into `reactive-transaction-api`.
* Tests should follow a readable Given / When / Then structure.
* Prefer testing public behavior over implementation details.

## Commit messages

Use short, imperative commit messages.

Examples:

```text
Add transaction timeout tests
Add Spring transaction adapter
Configure GitHub Actions CI
```

## Pull request rules

A pull request should:

* Explain what changed and why.
* Include tests for new behavior.
* Keep unrelated changes out of the PR.
* Pass formatting checks.
* Pass the full build.
* Avoid introducing new public API without discussion.

## Module boundaries

### `reactive-transaction-api`

This module contains the Spring-independent public API.

It must not depend on Spring Framework, Spring Boot, R2DBC, Testcontainers, or database drivers.

### `reactive-transaction-spring`

This module contains the Spring Framework adapter.

It may depend on Spring transaction infrastructure, but application-facing code should still use the library API.

## Testing strategy

Unit tests should verify public API contracts and adapter behavior.

Integration tests should verify real transaction behavior against real databases. PostgreSQL with Testcontainers is the first planned target.

## Versioning

The project uses semantic versioning.

Before `1.0.0`, public API may still change. After `1.0.0`, breaking changes should require a major version bump.
