# Spring Reactive Transaction Boundary

[![CI](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml)
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=CamilYed_spring-reactive-transaction-boundary)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring-Reactive-green.svg)](https://spring.io/projects/spring-framework)
[![Reactor](https://img.shields.io/badge/Reactor-Mono%20%7C%20Flux-blueviolet.svg)](https://projectreactor.io/)

Application-level transaction boundary API for reactive Spring applications.

`spring-reactive-transaction-boundary` lets application services run Reactor `Mono` and `Flux` workflows inside Spring-managed transactions without depending directly on Spring's `TransactionalOperator`.

It is designed for Spring WebFlux and R2DBC applications that want explicit, programmatic transaction boundaries while keeping infrastructure APIs out of use cases, domain-facing services, and Clean/Hexagonal Architecture application layers.

## Why this exists

Spring already provides excellent reactive transaction support through `TransactionalOperator`.

However, using `TransactionalOperator` directly in application services makes the use case depend on Spring transaction infrastructure:

```java
final class CreateOrderUseCase {

  private final TransactionalOperator transactionalOperator;
  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;

  Mono<OrderId> handle(CreateOrder command) {
    return orderRepository
        .save(command.toOrder())
        .flatMap(order -> paymentRepository.reserveFor(order).thenReturn(order.id()))
        .as(transactionalOperator::transactional);
  }
}
```

This works, but the application layer now knows about Spring's transaction API.

This library moves that infrastructure detail behind a small application-facing abstraction:

```java
final class CreateOrderUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;

  Mono<OrderId> handle(CreateOrder command) {
    return transaction.inTransaction(
        () ->
            orderRepository
                .save(command.toOrder())
                .flatMap(order -> paymentRepository.reserveFor(order).thenReturn(order.id())));
  }
}
```

The use case still declares the transaction boundary explicitly, but it no longer depends on `TransactionalOperator`.

## Core idea

```text
Application service
        |
        v
ReactiveTransaction
        |
        v
Spring adapter
        |
        v
ReactiveTransactionManager / TransactionalOperator
        |
        v
R2DBC transaction
```

Spring remains the transaction engine. The application layer depends on a small boundary interface.

## Features

- Spring-independent public API for application code
- Spring Framework adapter based on `ReactiveTransactionManager`
- Spring Boot auto-configuration
- Dedicated APIs for `Mono` and `Flux`
- Lazy operation creation through suppliers
- Transaction options for propagation, isolation, read-only mode, and timeout
- Test-friendly application services without Spring test context
- Real PostgreSQL/R2DBC integration coverage with Testcontainers
- Standalone WebFlux/R2DBC DDD demo application

## Project status

The project is in active development.

The API is intentionally small and already usable for experiments, demos, and internal projects. Treat it as pre-stable until the first public release. Breaking changes may happen before `1.0.0`.

Current snapshot version:

```text
0.1.0-SNAPSHOT
```

## Requirements

| Requirement | Version |
| --- | --- |
| Java | 21 or newer |
| Reactor | Provided transitively by Spring/Reactor dependencies |
| Spring Framework | Required by `reactive-transaction-spring` |
| Spring Boot | Required by the starter and auto-configuration modules |
| Build tool | Gradle Wrapper |

## Modules

| Module | Purpose |
| --- | --- |
| `reactive-transaction-api` | Spring-independent public API used by application code. |
| `reactive-transaction-spring` | Spring adapter implemented on top of `ReactiveTransactionManager` and `TransactionalOperator`. |
| `reactive-transaction-spring-boot-autoconfigure` | Auto-configuration that creates the default `ReactiveTransaction` bean. |
| `reactive-transaction-spring-boot-starter` | Convenience starter that brings the Spring adapter and auto-configuration together. |
| `examples/spring-boot-webflux-r2dbc-ddd-demo` | Standalone WebFlux/R2DBC/PostgreSQL demo consuming the published snapshot. |

## Installation

Snapshots are published to the Sonatype Central Portal snapshots repository.

```kotlin
repositories {
  mavenCentral()
  maven {
    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
  }
}
```

### Spring Boot starter

For Spring Boot applications, use the starter:

```kotlin
dependencies {
  implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT")
}
```

The starter auto-configures a `ReactiveTransaction` bean when a Spring `ReactiveTransactionManager` bean is available.

Your application still owns its database infrastructure: R2DBC driver, connection configuration, migrations/schema initialization, and transaction manager setup when Spring Boot does not provide one automatically.

### Manual Spring adapter

Use the Spring adapter directly when you do not want Spring Boot auto-configuration:

```kotlin
dependencies {
  implementation("io.github.camilyed:reactive-transaction-api:0.1.0-SNAPSHOT")
  implementation("io.github.camilyed:reactive-transaction-spring:0.1.0-SNAPSHOT")
}
```

Then register the adapter yourself:

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

## Quick start with Spring Boot

Inject `ReactiveTransaction` into an application service:

```java
import io.github.camilyed.transaction.ReactiveTransaction;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
final class CreateOrderUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;
  private final PaymentReservationRepository paymentReservations;

  CreateOrderUseCase(
      ReactiveTransaction transaction,
      OrderRepository orderRepository,
      PaymentReservationRepository paymentReservations) {
    this.transaction = transaction;
    this.orderRepository = orderRepository;
    this.paymentReservations = paymentReservations;
  }

  Mono<OrderId> handle(CreateOrder command) {
    return transaction.inTransaction(
        () -> {
          var order = Order.newOrder(command.customerId(), command.amount());

          return orderRepository
              .save(order)
              .flatMap(
                  savedOrder ->
                      paymentReservations.reserveFor(savedOrder)
                          .thenReturn(savedOrder.id()));
        });
  }
}
```

The application service expresses the business workflow and consistency boundary. Spring transaction infrastructure remains behind the adapter.

## `Mono` transactions

Use `inTransaction(...)` for operations that return one value or complete empty:

```java
Mono<OrderId> handle(CreateOrder command) {
  return transaction.inTransaction(
      () ->
          orderRepository
              .save(command.toOrder())
              .flatMap(order -> paymentRepository.reserveFor(order).thenReturn(order.id())));
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
              .flatMap(order -> paymentRepository.reserveFor(order).thenReturn(order.id())));
}
```

## `Flux` transactions

Use `inTransactionMany(...)` for operations that return multiple values:

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

The separate method is intentional. Java type erasure makes overloads based only on `Supplier<Mono<T>>` and `Supplier<Flux<T>>` difficult to expose cleanly.

## Transaction options

`TransactionOptions` lets application code configure transaction behavior without importing Spring transaction classes.

```java
var options =
    TransactionOptions.defaults()
        .withPropagation(Propagation.REQUIRES_NEW)
        .withIsolation(Isolation.SERIALIZABLE)
        .withReadOnly()
        .withTimeout(Duration.ofSeconds(5));
```

Supported options:

| Option | Purpose |
| --- | --- |
| `Propagation` | Defines how the operation participates in an existing transaction context. |
| `Isolation` | Defines the transaction isolation level. |
| `readOnly` | Marks the transaction as read-only where supported by the transaction manager. |
| `timeout` | Defines an explicit transaction timeout or uses the transaction manager default. |

Convenience factory:

```java
TransactionOptions.serializableNewTransaction()
```

This is useful for operations that must run in a new transaction with strict isolation.

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

The supplier-based API makes the transaction boundary explicit and allows the adapter to create the reactive pipeline inside the transactional scope.

This matters in reactive code because context propagation and resource binding happen through the reactive pipeline.

## Clean Architecture and Hexagonal Architecture

The application layer can depend on ports and the transaction boundary:

```text
application service
        |
        v
ReactiveTransaction   OrderRepository   PaymentReservationRepository
        |                  |                       |
        v                  v                       v
Spring adapter       R2DBC adapter           external adapter
```

The application service does not import Spring transaction APIs:

```java
final class RegisterPaymentUseCase {

  private final ReactiveTransaction transaction;
  private final PaymentRepository payments;
  private final Ledger ledger;

  Mono<PaymentId> handle(RegisterPayment command) {
    return transaction.inTransaction(
        () ->
            payments
                .save(command.toPayment())
                .flatMap(payment -> ledger.record(payment).thenReturn(payment.id())));
  }
}
```

Infrastructure decides how the boundary is implemented:

```java
@Bean
ReactiveTransaction reactiveTransaction(ReactiveTransactionManager transactionManager) {
  return new SpringReactiveTransaction(transactionManager);
}
```

Or, in Spring Boot, the starter can provide this bean automatically.

## Spring Boot auto-configuration

The starter auto-configures `ReactiveTransaction` when a Spring `ReactiveTransactionManager` is present:

```text
ReactiveTransactionManager bean exists
        |
        v
ReactiveTransactionAutoConfiguration
        |
        v
ReactiveTransaction bean
```

The auto-configuration backs off when the application provides its own `ReactiveTransaction` bean:

```java
@Bean
ReactiveTransaction customReactiveTransaction() {
  return new CustomReactiveTransaction();
}
```

This keeps the starter safe for applications that need custom behavior.

## R2DBC transaction manager

For R2DBC applications, Spring's transaction engine is typically `R2dbcTransactionManager`.

Depending on your Spring Boot setup, Boot may create it automatically. If not, define it explicitly:

```java
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
class R2dbcTransactionManagerConfiguration {

  @Bean
  ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }
}
```

The library does not create database-specific transaction managers by itself. It adapts an existing Spring `ReactiveTransactionManager`.

## Demo application

A complete standalone demo is available under:

```text
examples/spring-boot-webflux-r2dbc-ddd-demo
```

The demo uses:

- Spring Boot WebFlux
- R2DBC
- PostgreSQL
- Testcontainers
- DDD-style use cases
- Hexagonal ports and adapters
- the published `0.1.0-SNAPSHOT` starter from Sonatype Central snapshots

Run the demo tests:

```bash
cd examples/spring-boot-webflux-r2dbc-ddd-demo
./gradlew clean test --refresh-dependencies
```

The demo proves that an external application can consume the remote snapshot and get a `ReactiveTransaction` bean from the Spring Boot starter.

## Testing application code without Spring

Application services can be tested without a Spring context by using a simple fake implementation:

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

## PostgreSQL and R2DBC test coverage

The Spring adapter module includes PostgreSQL integration tests with Testcontainers.

The tests verify:

- commit for `Mono`
- rollback for `Mono`
- commit for `Flux`
- rollback for `Flux`
- read-only transaction options
- serializable isolation
- `REQUIRED` propagation
- `REQUIRES_NEW` propagation
- PostgreSQL transaction diagnostics

The public API module remains Spring-independent and database-independent.

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

Apply formatting:

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

## CI

The root build and the standalone demo should both be tested.

Example GitHub Actions steps:

```yaml
- name: Build root project
  run: ./gradlew clean build jacocoTestReport

- name: Test WebFlux R2DBC DDD demo
  working-directory: examples/spring-boot-webflux-r2dbc-ddd-demo
  run: ./gradlew clean test --refresh-dependencies
```

The demo step requires Docker because it uses Testcontainers with PostgreSQL.

## Publishing snapshots

Snapshots are published to the Sonatype Central Portal snapshots repository.

Use `--refresh-dependencies` in consumer projects when a fresh timestamped snapshot was published:

```bash
./gradlew clean test --refresh-dependencies
```

## Optional Git hooks

The repository provides an optional pre-commit hook that runs Spotless before each commit.

macOS, Linux, or Git Bash:

```bash
./scripts/install-git-hooks.sh
```

Windows PowerShell:

```powershell
.\scripts\install-git-hooks.ps1
```

The hook runs `./gradlew spotlessApply` and stages formatting changes before the commit.

## Design principles

| Principle | Description |
| --- | --- |
| Small public API | Keep the application-facing API minimal and explicit. |
| Spring isolation | Keep Spring-specific types outside `reactive-transaction-api`. |
| Lazy execution | Create reactive operations inside the transaction boundary. |
| Programmatic boundary | Support use cases where annotations are not expressive enough. |
| Testability | Allow application services to be tested without Spring transaction infrastructure. |
| Reactive-first | Support both `Mono` and `Flux` without blocking APIs. |
| Boot-friendly | Provide auto-configuration without forcing database drivers into the core API. |

## Roadmap

Near-term work:

- Publish the first public `0.1.0` release
- Improve documentation and examples
- Add wiki pages for Spring Boot usage and transaction options
- Add more diagnostics around database capabilities and transaction settings

Later ideas:

- Kotlin extension module
- Kotlin-friendly DSL
- Database-specific timeout helpers
- Additional Spring Boot diagnostics
- Blog article with a real Spring WebFlux/R2DBC use case

## License

This project is licensed under the Apache License 2.0.
