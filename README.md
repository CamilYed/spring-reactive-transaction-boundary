# Spring Reactive Transaction Boundary

[![CI](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/CamilYed/spring-reactive-transaction-boundary/actions/workflows/ci.yml)
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=CamilYed_spring-reactive-transaction-boundary)](https://sonarcloud.io/summary/new_code?id=CamilYed_spring-reactive-transaction-boundary)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

Application-level transaction boundary API for reactive Spring applications.

This project provides a small Java API for executing Reactor `Mono` operations inside a transaction without exposing Spring's `TransactionalOperator` to application services and use cases.

## Project status

The project is in active development.

The API is intentionally small, but it should still be treated as pre-stable until the first public release. Breaking changes may happen before `1.0.0`.

## Why this library exists

Spring Framework already provides reactive transaction support through `TransactionalOperator`. That API is powerful, but using it directly inside application services couples business-level code to Spring transaction infrastructure.

This library introduces a dedicated transaction boundary abstraction:

```java
ReactiveTransaction transaction;
```

Application code depends on that abstraction, while the Spring-specific transaction infrastructure stays behind an adapter.

The goal is not to replace Spring transactions. The goal is to make transaction boundaries explicit at the application layer and keep infrastructure details out of use cases.

## Modules

| Module | Description |
| --- | --- |
| `reactive-transaction-api` | Spring-independent public API. |
| `reactive-transaction-spring` | Spring Framework adapter based on `ReactiveTransactionManager` and `TransactionalOperator`. |

## Requirements

| Requirement | Version |
| --- | --- |
| Java | 21 or newer |
| Reactor | Provided by the project dependencies |
| Spring Framework | Required only by `reactive-transaction-spring` |
| Build tool | Gradle Wrapper |

## Installation

Maven Central publishing is planned. Until the first release is available, the project can be built locally:

```bash
./gradlew clean build
```

For local Maven usage during development:

```bash
./gradlew publishToMavenLocal
```

Then add the modules from your local Maven repository.

## Quick start

### Application service

```java
import io.github.softwarej.transaction.ReactiveTransaction;
import io.github.softwarej.transaction.TransactionOptions;
import reactor.core.publisher.Mono;

final class CreateOrderUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;

  CreateOrderUseCase(ReactiveTransaction transaction, OrderRepository orderRepository) {
    this.transaction = transaction;
    this.orderRepository = orderRepository;
  }

  Mono<OrderId> handle(CreateOrder command) {
    return transaction.inTransaction(
        TransactionOptions.serializableNewTransaction(),
        () -> orderRepository.save(command.toOrder()).map(Order::id));
  }
}
```

### Spring configuration

```java
import io.github.softwarej.transaction.ReactiveTransaction;
import io.github.softwarej.transaction.spring.SpringReactiveTransaction;
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

## Transaction options

`TransactionOptions` allows application code to configure common transaction behavior without depending on Spring classes.

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
| `Propagation` | Defines how the transaction participates in existing transaction context. |
| `readOnly` | Marks the transaction as read-only where supported by the underlying transaction manager. |
| `timeout` | Defines an explicit transaction timeout or uses the transaction manager default. |

## Lazy operation creation

The operation is passed as a `Supplier<Mono<T>>`, not as an already-created `Mono<T>`.

```java
transaction.inTransaction(() -> repository.save(entity));
```

This is intentional. The transaction implementation can establish the transaction boundary before the reactive pipeline is created and subscribed to.

This avoids accidentally creating part of the reactive chain outside the intended transaction boundary.

## Design principles

The project follows a few simple rules:

| Principle | Description |
| --- | --- |
| Small public API | Keep the application-facing API minimal and explicit. |
| Infrastructure isolation | Keep Spring-specific types outside `reactive-transaction-api`. |
| Lazy execution | Create reactive operations inside the transaction boundary. |
| No annotation requirement | Allow programmatic transaction boundaries where annotations are not a good fit. |
| Testable application code | Make transaction behavior easy to fake in unit tests. |

## Development

Build the project:

```bash
./gradlew clean build
```

Run tests:

```bash
./gradlew test
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

- PostgreSQL integration tests with R2DBC and Testcontainers
- Spring Boot auto-configuration
- Spring Boot starter module
- Maven Central publishing
- Improved documentation and examples

Later ideas:

- Kotlin extension module
- Kotlin-friendly DSL
- More examples for application-layer transaction boundaries
- Blog article with a real Spring WebFlux/R2DBC use case

## License

This project is licensed under the Apache License 2.0.
