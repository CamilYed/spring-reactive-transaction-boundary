# Spring Reactive Transaction Boundary

[![CI](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.camilyed/reactive-transaction-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.camilyed/reactive-transaction-spring-boot-starter)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_spring-reactive-transaction-boundary&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_spring-reactive-transaction-boundary&metric=coverage)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_spring-reactive-transaction-boundary&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_spring-reactive-transaction-boundary&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=CamilYed_spring-reactive-transaction-boundary&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Framework](https://img.shields.io/badge/Spring%20Framework-7.x-6DB33F.svg)](https://spring.io/projects/spring-framework)
[![Reactor](https://img.shields.io/badge/Reactor-Mono%20%7C%20Flux-blueviolet.svg)](https://projectreactor.io/)
[![PostgreSQL](https://img.shields.io/badge/tested%20with-PostgreSQL%20R2DBC-336791.svg)](https://www.postgresql.org/)

Application-level transaction boundary API for reactive Spring applications.

`spring-reactive-transaction-boundary` lets application services run Reactor `Mono` and `Flux`
workflows inside Spring-managed transactions without depending directly on Spring's
`TransactionalOperator`.

It is designed for Spring WebFlux and R2DBC applications that use explicit application services,
ports and adapters, Clean Architecture, Hexagonal Architecture, or DDD-style use cases.

```java
final class CreateOrderUseCase {

    private final ReactiveTransaction transaction;
    private final OrderRepository orderRepository;
    private final PaymentReservationRepository paymentReservations;

    Mono<OrderId> handle(CreateOrder command) {
        return transaction.inTransaction(
                () ->
                        orderRepository
                                .save(command.toOrder())
                                .flatMap(order -> paymentReservations.reserveFor(order).thenReturn(order.id())));
    }
}
```

Spring remains the transaction engine. Your application layer depends only on a small boundary
interface.

## Contents

- [Why](#why)
- [Features](#features)
- [Status](#status)
- [Requirements](#requirements)
- [Modules](#modules)
- [Installation](#installation)
- [Snapshots](#snapshots)
- [Spring Boot setup](#spring-boot-setup)
- [Usage](#usage)
	- [`Mono`](#mono)
	- [`Flux`](#flux)
- [Transaction options](#transaction-options)
- [Lazy operation creation](#lazy-operation-creation)
- [Spring Boot auto-configuration](#spring-boot-auto-configuration)
- [Testing application code without Spring](#testing-application-code-without-spring)
- [PostgreSQL and R2DBC verification](#postgresql-and-r2dbc-verification)
- [Demo application](#demo-application)
- [Examples and code map](#examples-and-code-map)
- [Development](#development)
- [What this library is not](#what-this-library-is-not)
- [Design principles](#design-principles)
- [Roadmap](#roadmap)

## Why

Spring already provides excellent reactive transaction support through `ReactiveTransactionManager`
and `TransactionalOperator`.

This library is useful when you want:

- explicit transaction boundaries in application services
- Spring transaction infrastructure outside the application layer
- a small API that can be faked in use case tests
- one abstraction for both `Mono` and `Flux` workflows
- transaction options without importing Spring transaction types into core application code

## Features

Main features:

- Spring-independent public API
- Spring adapter backed by `ReactiveTransactionManager`
- Spring Boot 4 auto-configuration and starter
- Dedicated APIs for `Mono` and `Flux`
- Lazy operation creation through `Supplier`
- Transaction options for propagation, isolation, read-only mode, and timeout

Additional features:

- Test-friendly application services without a Spring test context
- PostgreSQL/R2DBC integration verification with Testcontainers
- Standalone Spring Boot WebFlux/R2DBC/PostgreSQL demo application
- Safe Spring Boot auto-configuration that backs off when a custom `ReactiveTransaction` exists

## Status

Current version:

```text
0.1.0
```

This is the first public release. The API is intentionally small and suitable for early production
evaluation, demos, and internal projects.

The project is still pre-`1.0.0`, so breaking changes may happen before `1.0.0`.

## Requirements

| Requirement | Version |
| --- | --- |
| JDK | 21 |
| Spring Boot | 4.x |
| Spring Framework | 7.x |
| Reactor | Provided by Spring/Reactor dependencies |
| Database runtime | R2DBC-based Spring `ReactiveTransactionManager` |
| Verified database | PostgreSQL with R2DBC and Testcontainers |

PostgreSQL is the first verified database. Other R2DBC databases may work through Spring's
`ReactiveTransactionManager`, but they are not yet part of the integration test matrix.

## Modules

| Module | Purpose |
| --- | --- |
| `reactive-transaction-api` | Spring-independent API used by application code. |
| `reactive-transaction-spring` | Spring adapter implemented on top of Spring reactive transaction infrastructure. |
| `reactive-transaction-spring-boot-autoconfigure` | Auto-configuration that creates the default `ReactiveTransaction` bean. |
| `reactive-transaction-spring-boot-starter` | Convenience starter for Spring Boot applications. |
| `examples/spring-boot-webflux-r2dbc-ddd-demo` | Standalone WebFlux/R2DBC/PostgreSQL demo consuming the published release. |

## Installation

Artifacts are published to Maven Central.

```kotlin
repositories {
  mavenCentral()
}
```

### Spring Boot starter

```kotlin
dependencies {
  implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0")
}
```

The starter creates a `ReactiveTransaction` bean when a Spring `ReactiveTransactionManager` bean is
available.

Your application still owns its database infrastructure: R2DBC driver, connection configuration,
migrations or schema initialization, and the transaction manager setup when Spring Boot does not
create one automatically.

### Manual Spring adapter

Use the adapter directly when you do not want Spring Boot auto-configuration:

```kotlin
dependencies {
  implementation("io.github.camilyed:reactive-transaction-api:0.1.0")
  implementation("io.github.camilyed:reactive-transaction-spring:0.1.0")
}
```

```java
import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.spring.SpringReactiveTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
class TransactionConfiguration {

  @Bean
  ReactiveTransaction reactiveTransaction(ReactiveTransactionManager transactionManager) {
    return new SpringReactiveTransaction(transactionManager);
  }
}
```

## Snapshots

Snapshot builds are published to the Sonatype Central Portal snapshots repository.

```kotlin
repositories {
  mavenCentral()
  maven {
    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
  }
}

dependencies {
  implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.1-SNAPSHOT")
}
```

Use snapshots only for short-lived testing.

## Spring Boot setup

For R2DBC applications, Spring's transaction engine is typically `R2dbcTransactionManager`.

Depending on your Spring Boot setup, Boot may create it automatically. If not, define it explicitly:

```java
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration(proxyBeanMethods = false)
class R2dbcTransactionManagerConfiguration {

  @Bean
  ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }
}
```

The library does not create database-specific transaction managers by itself. It adapts an existing
Spring `ReactiveTransactionManager`.

## Usage

### `Mono`

Use `inTransaction(...)` for workflows that return one value or complete empty:

```java
Mono<OrderId> handle(CreateOrder command) {
  return transaction.inTransaction(
      () ->
          orderRepository
              .save(command.toOrder())
              .flatMap(order -> paymentReservations.reserveFor(order).thenReturn(order.id())));
}
```

With explicit options:

```java
Mono<OrderId> handle(CreateOrder command) {
  return transaction.inTransaction(
      TransactionOptions.serializableNewTransaction(),
      () ->
          orderRepository
              .save(command.toOrder())
              .flatMap(order -> paymentReservations.reserveFor(order).thenReturn(order.id())));
}
```

### `Flux`

Use `inTransactionMany(...)` for workflows that return multiple values:

```java
Flux<OrderProjection> rebuild() {
  return transaction.inTransactionMany(
      () ->
          projectionRepository
              .deleteAll()
              .thenMany(orderRepository.findAll())
              .concatMap(projectionRepository::rebuildFrom));
}
```

The separate method is intentional. Java type erasure makes overloads based only on
`Supplier<Mono<T>>` and `Supplier<Flux<T>>` difficult to expose cleanly.

## Transaction options

`TransactionOptions` lets application code configure transaction behavior without importing Spring
transaction classes.

```java
var options =
    TransactionOptions.defaults()
        .withPropagation(Propagation.REQUIRES_NEW)
        .withIsolation(Isolation.SERIALIZABLE)
        .withReadOnly()
        .withTimeout(Duration.ofSeconds(5));
```

| Option | Purpose |
| --- | --- |
| `Propagation` | Defines how the operation participates in an existing transaction context. |
| `Isolation` | Defines the transaction isolation level. |
| `readOnly` | Marks the transaction as read-only where supported by the transaction manager and database. |
| `timeout` | Defines an explicit transaction timeout or uses the transaction manager default. |

Convenience factory:

```java
TransactionOptions.serializableNewTransaction()
```

Timeout is delegated to Spring's transaction infrastructure. Database-specific enforcement depends
on the configured transaction manager and database. PostgreSQL-specific timeout behavior can be
implemented by applying `SET LOCAL statement_timeout` on the transactional connection.

## Lazy operation creation

Operations are passed as suppliers:

```java
transaction.inTransaction(() -> repository.save(entity));
transaction.inTransactionMany(() -> repository.findAllForUpdate());
```

Avoid assembling the publisher before entering the transaction boundary:

```java
Mono<Entity> operation = repository.save(entity);

transaction.inTransaction(() -> operation);
```

The supplier-based API keeps reactive pipeline creation inside the transactional scope, where Reactor
context and transaction-bound resources can be applied correctly.

## Spring Boot auto-configuration

The starter auto-configures `ReactiveTransaction` when a `ReactiveTransactionManager` is present.

It backs off when the application provides its own `ReactiveTransaction` bean:

```java
@Bean
ReactiveTransaction customReactiveTransaction() {
  return new CustomReactiveTransaction();
}
```

## Testing application code without Spring

Application services can be tested without a Spring context:

```java
import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.TransactionOptions;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class ImmediateReactiveTransaction implements ReactiveTransaction {

  @Override
  public <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation) {
    return operation.get();
  }

  @Override
  public <T> Flux<T> inTransactionMany(TransactionOptions options, Supplier<Flux<T>> operation) {
    return operation.get();
  }
}
```

This keeps use case tests focused on business behavior instead of Spring transaction setup.

## PostgreSQL and R2DBC verification

The Spring adapter module includes PostgreSQL integration tests with Testcontainers.

PostgreSQL is the first verified database target. Other R2DBC databases may work through Spring's
`ReactiveTransactionManager`, but they are not yet part of the integration test matrix.

### Integration test matrix

| Capability | PostgreSQL | What is verified |
| --- | :---: | --- |
| `Mono` transaction commit | ✅ | Data is persisted when the operation completes successfully. |
| `Mono` transaction rollback | ✅ | Data is rolled back when the operation fails. |
| `Flux` transaction commit | ✅ | Multi-value workflows commit as one transaction. |
| `Flux` transaction rollback | ✅ | Multi-value workflows roll back as one transaction on error. |
| Read-only transaction | ✅ | PostgreSQL `transaction_read_only` is enabled. |
| Read-only write rejection | ✅ | PostgreSQL rejects writes inside a read-only transaction. |
| Isolation mapping | ✅ | All public `Isolation` values are applied and observed in PostgreSQL. |
| Propagation behavior | ✅ | All public `Propagation` values are exercised against PostgreSQL. |
| Timeout behavior | ✅ | PostgreSQL `statement_timeout` is applied on the transactional connection. |
| Transaction diagnostics | ✅ | Read-only, isolation, statement timeout, lock timeout, and idle timeout are visible. |

Verified isolation values:

| API value | PostgreSQL behavior |
| --- | --- |
| `DEFAULT` | Uses PostgreSQL default isolation, usually `read committed`. |
| `READ_UNCOMMITTED` | Applied through Spring/R2DBC and observed from PostgreSQL. |
| `READ_COMMITTED` | Applied and observed as `read committed`. |
| `REPEATABLE_READ` | Applied and observed as `repeatable read`. |
| `SERIALIZABLE` | Applied and observed as `serializable`. |

Verified propagation values:

| API value | PostgreSQL scenario |
| --- | --- |
| `REQUIRED` | Joins an existing transaction and rolls back with it. |
| `REQUIRES_NEW` | Commits independently when the outer transaction rolls back. |
| `SUPPORTS` | Runs without a transaction, and joins when one exists. |
| `MANDATORY` | Fails without an existing transaction and works inside one. |
| `NOT_SUPPORTED` | Suspends an existing transaction and commits independently. |
| `NEVER` | Works without a transaction and fails inside an existing one. |
| `NESTED` | Verified against PostgreSQL nested transaction behavior through Spring/R2DBC. |

### Unit test matrix

| Area | Status | What is verified |
| --- | :---: | --- |
| API defaults | ✅ | Default options for `Mono` and `Flux` boundaries. |
| Spring mapping | ✅ | Public API options are mapped to Spring `TransactionDefinition`. |
| Isolation mapping | ✅ | All supported `Isolation` values map to Spring isolation constants. |
| Propagation mapping | ✅ | All supported `Propagation` values map to Spring propagation constants. |
| Timeout mapping | ✅ | `Duration` values are rounded to Spring's second-based timeout value. |
| Read-only mode | ✅ | The read-only flag is passed to Spring transaction infrastructure. |
| Lazy execution | ✅ | Reactive operations are created inside the transaction boundary. |
| Transaction outcome | ✅ | Commit and rollback behavior is verified for `Mono` and `Flux`. |
| Boot auto-configuration | ✅ | Bean creation, backoff, and missing-manager behavior are verified. |

The public API module remains Spring-independent and database-independent.

## Demo application

A standalone demo is available under:

```text
examples/spring-boot-webflux-r2dbc-ddd-demo
```

The demo uses Spring Boot WebFlux, R2DBC, PostgreSQL, Testcontainers, DDD-style use cases, and
Hexagonal ports and adapters.

Run the demo tests:

```bash
cd examples/spring-boot-webflux-r2dbc-ddd-demo
./gradlew clean test --refresh-dependencies
```

The demo proves that an external Spring Boot application can consume the published release and get a
`ReactiveTransaction` bean from the starter.

## Examples and code map

The README keeps examples short. The repository contains a full external Spring Boot demo and
test-backed reference points for deeper inspection.

| Topic | Path |
| --- | --- |
| Standalone Spring Boot WebFlux/R2DBC demo | [`examples/spring-boot-webflux-r2dbc-ddd-demo`](examples/spring-boot-webflux-r2dbc-ddd-demo) |
| Demo application entry point | [`examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/TransactionDemoApplication.java`](examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/TransactionDemoApplication.java) |
| Demo use cases | [`examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/application`](examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/application) |
| Demo ports | [`examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/application/port`](examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/application/port) |
| Demo R2DBC adapters | [`examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/adapter/out/r2dbc`](examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/adapter/out/r2dbc) |
| Demo WebFlux controller | [`examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/adapter/in/web`](examples/spring-boot-webflux-r2dbc-ddd-demo/src/main/java/io/github/camilyed/transaction/demo/order/adapter/in/web) |
| PostgreSQL integration test matrix | [`reactive-transaction-spring/src/test/java/io/github/camilyed/transaction/spring/integration/SpringReactiveTransactionPostgreSqlIntegrationTest.java`](reactive-transaction-spring/src/test/java/io/github/camilyed/transaction/spring/integration/SpringReactiveTransactionPostgreSqlIntegrationTest.java) |
| Spring adapter unit tests | [`reactive-transaction-spring/src/test/java/io/github/camilyed/transaction/spring/SpringReactiveTransactionTest.java`](reactive-transaction-spring/src/test/java/io/github/camilyed/transaction/spring/SpringReactiveTransactionTest.java) |
| Boot auto-configuration tests | [`reactive-transaction-spring-boot-autoconfigure/src/test/java/io/github/camilyed/transaction/spring/boot/autoconfigure/ReactiveTransactionAutoConfigurationTest.java`](reactive-transaction-spring-boot-autoconfigure/src/test/java/io/github/camilyed/transaction/spring/boot/autoconfigure/ReactiveTransactionAutoConfigurationTest.java) |

## Development

Build everything:

```bash
./gradlew clean build
```

Run all root project tests:

```bash
./gradlew test
```

Run module tests:

```bash
./gradlew :reactive-transaction-api:test
./gradlew :reactive-transaction-spring:test
./gradlew :reactive-transaction-spring-boot-autoconfigure:test
```

Run the standalone demo tests:

```bash
cd examples/spring-boot-webflux-r2dbc-ddd-demo
./gradlew clean test --refresh-dependencies
```

Format code:

```bash
./gradlew spotlessApply
```

Check formatting:

```bash
./gradlew spotlessCheck
```

Generate coverage reports:

```bash
./gradlew jacocoTestReport
```

Docker must be available for Testcontainers-based integration tests.

## What this library is not

- It is not a database migration tool.
- It is not a replacement for Spring's transaction engine.
- It is not a custom R2DBC transaction manager.
- It does not hide transaction boundaries behind annotations.
- It does not make database-specific transaction semantics identical across vendors.

## Design principles

| Principle | Description |
| --- | --- |
| Small public API | Keep the application-facing API minimal and explicit. |
| Spring isolation | Keep Spring-specific types outside `reactive-transaction-api`. |
| Reactive-first | Support both `Mono` and `Flux` without blocking APIs. |
| Lazy execution | Create reactive operations inside the transaction boundary. |
| Programmatic boundary | Support use cases where annotations are not expressive enough. |
| Testability | Allow application services to be tested without Spring transaction infrastructure. |
| Boot-friendly | Provide auto-configuration without forcing database drivers into the core API. |

## Roadmap

Near-term:

- Patch releases for the `0.1.x` line
- Post-release smoke testing from Maven Central
- Expand documentation for Spring Boot usage and transaction options
- Keep PostgreSQL as the first verified database target

Later ideas:

- Kotlin extension module
- Kotlin-friendly DSL
- Additional database compatibility tests
- Database-specific timeout helpers
- Additional Spring Boot diagnostics
- Blog article with a real Spring WebFlux/R2DBC use case

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

Before opening a pull request, run:

```bash
./gradlew spotlessCheck clean build
```

## License

This project is licensed under the Apache License 2.0.
