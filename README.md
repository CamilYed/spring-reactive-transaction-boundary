# Spring Reactive Transaction Boundary

[![CI](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml)
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=CamilYed_spring-reactive-transaction-boundary)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

Application-level transaction boundary API for reactive Spring applications.

Spring Reactive Transaction Boundary provides a small Java API for executing Reactor `Mono` and `Flux` operations inside a transaction without exposing Spring's `TransactionalOperator` to application services, use cases, or domain-facing code.

The library is intended for WebFlux and R2DBC applications that prefer explicit application-layer transaction boundaries over direct infrastructure usage in business workflows.

## Project status

The project is in active development.

The API is intentionally small and already usable for experiments and internal projects, but it should still be treated as pre-stable until the first public release. Breaking changes may happen before `1.0.0`.

## Problem

Spring Framework already provides reactive transaction support through `TransactionalOperator`. It is a powerful infrastructure API, but using it directly in application services can leak Spring transaction concerns into code that should describe business workflows.

This often leads to application services that know too much about infrastructure:

```java
final class CreateOrderUseCase {

    private final TransactionalOperator transactionalOperator;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    Mono<OrderId> handle(CreateOrder command) {
        return orderRepository
                .save(command.toOrder())
                .flatMap(order -> paymentRepository.reserve(order).thenReturn(order.id()))
                .as(transactionalOperator::transactional);
    }
}
```

The code works, but the use case now depends directly on Spring transaction infrastructure. In small applications this may be acceptable. In larger systems, especially those using Clean Architecture, Hexagonal Architecture, or DDD-style application layers, this coupling becomes harder to maintain and harder to test.

## What this library provides

The library introduces one application-facing transaction boundary:

```java
ReactiveTransaction transaction;
```

Application code asks for a transaction boundary. The Spring adapter decides how that boundary is implemented.

```java
return transaction.inTransaction(
    () ->
        orderRepository
        .save(order)
            .flatMap(savedOrder -> paymentRepository.reserve(savedOrder).thenReturn(savedOrder.id())));
```

The goal is not to replace Spring transactions. The goal is to keep Spring transaction infrastructure behind an adapter while making transaction boundaries explicit in the application layer.

## Supported reactive types

| Operation type | API method | Reactor type |
| --- | --- | --- |
| Single result | `inTransaction(...)` | `Mono<T>` |
| Multiple results | `inTransactionMany(...)` | `Flux<T>` |

The separate `inTransactionMany(...)` method is intentional. Java type erasure makes overloads based only on `Supplier<Mono<T>>` and `Supplier<Flux<T>>` ambiguous at runtime and difficult to expose cleanly.

## Modules

| Module | Description |
| --- | --- |
| `reactive-transaction-api` | Spring-independent public API used by application code. |
| `reactive-transaction-spring` | Spring Framework adapter based on `ReactiveTransactionManager` and `TransactionalOperator`. |
| `reactive-transaction-spring-boot-autoconfigure` | Spring Boot auto-configuration for the default `ReactiveTransaction` bean. |
| `reactive-transaction-spring-boot-starter` | Convenience starter that brings the Spring adapter and auto-configuration together. |

## Requirements

| Requirement | Version |
| --- | --- |
| Java | 21 or newer |
| Reactor | Provided by project dependencies |
| Spring Framework | Required by `reactive-transaction-spring` |
| Spring Boot | Required by the Spring Boot starter and auto-configuration modules |
| Build tool | Gradle Wrapper |

## Installation

Snapshots are published to the Sonatype Central Portal snapshots repository.

For snapshot usage, add the snapshots repository:

```kotlin
repositories {
  mavenCentral()
  maven {
    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
  }
}
```

### Spring Boot starter

For Spring Boot applications, prefer the starter:

```kotlin
dependencies {
  implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT")
}
```

The starter auto-configures a `ReactiveTransaction` bean when a Spring `ReactiveTransactionManager` bean is available.

The application still owns its database driver and R2DBC configuration. For example, a PostgreSQL application still needs its R2DBC driver and connection configuration from Spring Boot or custom infrastructure code.

### Manual Spring adapter

Use the Spring adapter directly when you do not want Spring Boot auto-configuration:

```kotlin
dependencies {
    implementation("io.github.camilyed:reactive-transaction-api:0.1.0-SNAPSHOT")
    implementation("io.github.camilyed:reactive-transaction-spring:0.1.0-SNAPSHOT")
}
```

## Quick start

### Spring Boot usage

When the starter is on the classpath, the application can inject `ReactiveTransaction` directly:

```java
import io.github.camilyed.transaction.ReactiveTransaction;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
final class CreateOrderUseCase {

    private final ReactiveTransaction transaction;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    CreateOrderUseCase(
            ReactiveTransaction transaction,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository) {
        this.transaction = transaction;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    Mono<OrderId> handle(CreateOrder command) {
        return transaction.inTransaction(
                () ->
                        orderRepository
                                .save(command.toOrder())
                                .flatMap(
                                        order ->
                                                paymentRepository
                                                        .reserveFor(order)
                                                        .thenReturn(order.id())));
    }
}
```

The auto-configuration backs off when the application provides its own `ReactiveTransaction` bean:

```java
@Bean
ReactiveTransaction customReactiveTransaction() {
    return new CustomReactiveTransaction();
}
```

### Manual Spring configuration

Register the Spring adapter as an application bean:

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

The rest of the application depends on `ReactiveTransaction`, not on `TransactionalOperator`.

### Application service with `Mono`

```java
import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.TransactionOptions;
import reactor.core.publisher.Mono;

final class CreateOrderUseCase {

    private final ReactiveTransaction transaction;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    CreateOrderUseCase(
            ReactiveTransaction transaction,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository) {
        this.transaction = transaction;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    Mono<OrderId> handle(CreateOrder command) {
        return transaction.inTransaction(
                TransactionOptions.serializableNewTransaction(),
                () ->
                        orderRepository
                                .save(command.toOrder())
                                .flatMap(
                                        order ->
                                                paymentRepository
                                                        .reserveFor(order)
                                                        .thenReturn(order.id())));
    }
}
```

### Application service with `Flux`

Use `inTransactionMany(...)` when the operation returns multiple values:

```java
import io.github.camilyed.transaction.ReactiveTransaction;
import reactor.core.publisher.Flux;

final class RebuildCustomerProjectionUseCase {

    private final ReactiveTransaction transaction;
    private final CustomerEventRepository eventRepository;
    private final CustomerProjectionRepository projectionRepository;

    RebuildCustomerProjectionUseCase(
            ReactiveTransaction transaction,
            CustomerEventRepository eventRepository,
            CustomerProjectionRepository projectionRepository) {
        this.transaction = transaction;
        this.eventRepository = eventRepository;
        this.projectionRepository = projectionRepository;
    }

    Flux<CustomerProjection> handle(CustomerId customerId) {
        return transaction.inTransactionMany(
                () ->
                        eventRepository
                                .findByCustomerId(customerId)
                                .concatMap(projectionRepository::apply));
    }
}
```

## Clean Architecture and Hexagonal Architecture

In Clean Architecture or Hexagonal Architecture, application services should orchestrate use cases and depend on abstractions. Infrastructure details should live at the edge of the system.

A typical dependency direction can look like this:

```text
application service
        |
        v
ReactiveTransaction   OrderRepository   PaymentRepository
        |                  |                 |
        v                  v                 v
Spring adapter       R2DBC adapter      external adapter
```

The application layer can express the transaction boundary without importing Spring transaction classes:

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

The infrastructure layer wires the implementation manually or through the Spring Boot starter:

```java
@Bean
ReactiveTransaction reactiveTransaction(ReactiveTransactionManager transactionManager) {
    return new SpringReactiveTransaction(transactionManager);
}
```

This keeps the application service focused on workflow and consistency boundaries. Spring remains the transaction engine, but it is no longer part of the use case API.

## Transaction options

`TransactionOptions` allows application code to configure transaction behavior without depending on Spring classes.

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
| `Isolation` | Defines the transaction isolation level. |
| `Propagation` | Defines how the operation participates in an existing transaction context. |
| `readOnly` | Marks the transaction as read-only where supported by the underlying transaction manager. |
| `timeout` | Defines an explicit transaction timeout or uses the transaction manager default. |

Convenience factory:

```java
TransactionOptions.serializableNewTransaction()
```

This is useful for operations that must run in a new transaction with strict isolation.

### Transaction timeout

`TransactionOptions.withTimeout(...)` is delegated to Spring's `TransactionDefinition`.

Database-specific statement timeouts are not configured by this library. For example, PostgreSQL `statement_timeout` is not automatically changed by `TransactionOptions.withTimeout(...)`.

Database-specific timeout diagnostics and configuration may be added in a future Spring Boot autoconfiguration or diagnostics module.

## PostgreSQL and R2DBC

The project includes integration tests against PostgreSQL using R2DBC and Testcontainers.

The tests verify that the Spring adapter works with a real relational database transaction manager, including:

- commit and rollback for `Mono`
- commit and rollback for `Flux`
- read-only transaction settings
- serializable isolation
- `REQUIRED` and `REQUIRES_NEW` propagation behavior
- PostgreSQL transaction setting diagnostics

This test coverage is intentionally placed in the Spring adapter module. The public API module remains Spring-independent and database-independent.

## Lazy operation creation

Operations are passed as suppliers:

```java
transaction.inTransaction(() -> repository.save(entity));
        transaction.inTransactionMany(() -> repository.findAllForUpdate());
```

They are not passed as already-created publishers:

```java
Mono<Entity> operation = repository.save(entity);
transaction.inTransaction(() -> operation);
```

The supplier-based API is intentional. It allows the adapter to create the publisher inside the transaction boundary.

This is important in reactive code because a pipeline can capture context at assembly time. Creating the pipeline lazily makes the transaction boundary explicit and reduces the risk of assembling part of the operation outside the intended transactional scope.

## Testing application code

Application services can be tested without Spring by providing a simple fake `ReactiveTransaction` implementation.

```java
final class ImmediateReactiveTransaction implements ReactiveTransaction {

    @Override
    public <T> Mono<T> inTransaction(
            TransactionOptions options,
            Supplier<Mono<T>> operation) {
        return operation.get();
    }

    @Override
    public <T> Flux<T> inTransactionMany(
            TransactionOptions options,
            Supplier<Flux<T>> operation) {
        return operation.get();
    }
}
```

This keeps use case tests focused on behavior instead of Spring transaction setup.

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

## Development

Build the project:

```bash
./gradlew clean build
```

Run all tests:

```bash
./gradlew test
```

Run module tests:

```bash
./gradlew :reactive-transaction-api:test
./gradlew :reactive-transaction-spring:test
./gradlew :reactive-transaction-spring-boot-autoconfigure:test
```

Apply formatting:

```bash
./gradlew spotlessApply
```

Check formatting:

```bash
./gradlew spotlessCheck
```

Run PostgreSQL integration tests:

```bash
./gradlew :reactive-transaction-spring:test
```

Docker must be available for Testcontainers-based integration tests.

Generate coverage reports:

```bash
./gradlew jacocoTestReport
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

## Roadmap

Near-term work:

- Maven Central publishing
- More documentation and examples
- Example Spring Boot WebFlux/R2DBC application
- Wiki pages for core concepts and Spring Boot usage

Later ideas:

- Kotlin extension module
- Kotlin-friendly DSL
- Database diagnostics and capability detection
- Database-specific timeout helpers
- Blog article with a real Spring WebFlux/R2DBC use case

## License

This project is licensed under the Apache License 2.0.
