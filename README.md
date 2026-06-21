# Spring Reactive Transaction Boundary

Application-level transaction boundary API for reactive Spring applications.

This library provides a small Java API for executing Reactor `Mono` operations within a transaction without depending directly on Spring's `TransactionalOperator` in application services and use cases.

## Status

Early development.

The public API is not stable yet and may change before the first stable release.

## Why?

Spring provides powerful reactive transaction support through `TransactionalOperator`, but application-level code often should not depend directly on infrastructure-specific transaction APIs.

This project introduces a small transaction boundary interface that can be used from use cases and application services while keeping Spring transaction infrastructure behind an adapter.

## Modules

| Module                        | Description                                                                                 |
| ----------------------------- | ------------------------------------------------------------------------------------------- |
| `reactive-transaction-api`    | Spring-independent public API.                                                              |
| `reactive-transaction-spring` | Spring Framework adapter based on `ReactiveTransactionManager` and `TransactionalOperator`. |

## Requirements

* Java 21
* Gradle Wrapper
* Spring Framework 6.x for the Spring adapter module

## Quick start

```java
import io.github.softwarej.transaction.ReactiveTransaction;
import io.github.softwarej.transaction.TransactionOptions;
import reactor.core.publisher.Mono;

final class CreateOrderUseCase {

    private final ReactiveTransaction transaction;
    private final OrderRepository orderRepository;

    CreateOrderUseCase(
            ReactiveTransaction transaction,
            OrderRepository orderRepository
    ) {
        this.transaction = transaction;
        this.orderRepository = orderRepository;
    }

    Mono<OrderId> handle(CreateOrder command) {
        return transaction.inTransaction(
                TransactionOptions.serializableNewTransaction(),
                () -> orderRepository.save(command.toOrder())
                        .map(Order::id)
        );
    }
}
```

## Design notes

The transaction operation is provided as a `Supplier<Mono<T>>` instead of a ready-made `Mono<T>`.

This allows the reactive chain to be created lazily by the transaction implementation, after the transaction boundary has been established.

## Build

```bash
./gradlew clean build
```

## Format

```bash
./gradlew spotlessApply
```

## Check formatting

```bash
./gradlew spotlessCheck
```

## Test

```bash
./gradlew test
```

## Roadmap

* Unit tests for the Spring adapter
* PostgreSQL integration tests with Testcontainers
* Spring Boot autoconfiguration
* Spring Boot starter
* Local Maven publishing
* Maven Central publishing
* Documentation and examples

## License

Apache License 2.0.
