# Spring Boot WebFlux R2DBC DDD Demo

Standalone Spring Boot demo application for `spring-reactive-transaction-boundary`.

The demo consumes the published `reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT`
from Sonatype Central snapshots and verifies that a real external application can use the starter
without depending on local Gradle project modules.

## What it demonstrates

| Area | Demonstrated behavior |
| --- | --- |
| Spring Boot integration | The starter auto-configures a `ReactiveTransaction` bean. |
| R2DBC transactions | A Spring `ReactiveTransactionManager` drives PostgreSQL transactions. |
| `Mono` boundary | `ReactiveTransaction.inTransaction(...)` commits and rolls back single-result workflows. |
| `Flux` boundary | `ReactiveTransaction.inTransactionMany(...)` commits and rolls back multi-result workflows. |
| Transaction options | Read-only mode, `REQUIRES_NEW` propagation, `SERIALIZABLE` isolation, and timeout options. |
| Architecture | DDD-style use cases with Hexagonal ports and adapters. |
| Verification | Integration tests run against real PostgreSQL through Testcontainers. |

The demo does **not** create a `ReactiveTransaction` bean manually. That bean must come from the
Spring Boot starter.

## Requirements

| Requirement | Version / note |
| --- | --- |
| JDK | 21 |
| Spring Boot | 4.x |
| Database | PostgreSQL |
| Docker | Required for Testcontainers and optional local `docker compose` run |
| Dependency source | Sonatype Central snapshots |

## Dependency setup

The demo intentionally resolves the library from the remote snapshot repository:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {
    implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT")
}
```

Use `--refresh-dependencies` after publishing a fresh snapshot.

## Architecture

```text
HTTP API
  -> application use case
    -> ReactiveTransaction
      -> SpringReactiveTransaction
        -> ReactiveTransactionManager
          -> R2DBC / PostgreSQL
```

Application services depend on the library abstraction:

```java
import io.github.camilyed.transaction.ReactiveTransaction;
```

They do not depend directly on Spring's `TransactionalOperator`.

## Project structure

```text
src/main/java/io/github/camilyed/transaction/demo
├── TransactionDemoApplication.java
├── config
│   └── R2dbcTransactionManagerConfiguration.java
└── order
    ├── adapter
    │   ├── in/web
    │   └── out/r2dbc
    ├── application
    │   ├── port
    │   ├── ConfirmOrderUseCase.java
    │   ├── CreateOrderUseCase.java
    │   ├── CreateOrderWithFailureUseCase.java
    │   ├── ListOrdersUseCase.java
    │   └── RebuildOrderProjectionUseCase.java
    └── domain
```

## Transaction manager

The application provides standard Spring database infrastructure:

```java
@Configuration(proxyBeanMethods = false)
public class R2dbcTransactionManagerConfiguration {

  @Bean
  public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }
}
```

The starter detects this `ReactiveTransactionManager` and creates the default `ReactiveTransaction`
adapter.

## Use cases

| Use case | Transaction behavior |
| --- | --- |
| `CreateOrderUseCase` | Creates an order and payment reservation in one `Mono` transaction. |
| `CreateOrderWithFailureUseCase` | Writes data and then fails, proving rollback behavior. |
| `ListOrdersUseCase` | Reads orders through a read-only transaction. |
| `ConfirmOrderUseCase` | Uses explicit propagation, isolation, and timeout options. |
| `RebuildOrderProjectionUseCase` | Rebuilds projections through a `Flux` transaction. |

## Running tests

The integration tests start PostgreSQL with Testcontainers.

```bash
./gradlew clean test --refresh-dependencies
```

Run only the starter smoke test:

```bash
./gradlew clean test --tests "*AutoConfigurationSmokeTest" --stacktrace
```

Check that the demo resolves the remote starter and not local project modules:

```bash
./gradlew dependencyInsight \
  --dependency reactive-transaction-spring-boot-starter \
  --configuration runtimeClasspath
```

Expected dependency:

```text
io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT
```

## Running locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./gradlew bootRun
```

The application starts on port `8080`.

## HTTP API

### Create an order

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-1","amount":49.99}'
```

### List orders

```bash
curl http://localhost:8080/orders
```

### Create an order and force rollback

```bash
curl -X POST http://localhost:8080/orders/failing \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-failing","amount":19.99}'
```

### Confirm an order

```bash
curl -X POST http://localhost:8080/orders/{orderId}/confirm
```

### Rebuild projections using a `Flux` transaction

```bash
curl -X POST http://localhost:8080/orders/projections/rebuild
```

### List projections

```bash
curl http://localhost:8080/orders/projections
```

## Expected behavior

| Scenario | Expected result |
| --- | --- |
| Successful order creation | One row in `demo_orders` and one row in `payment_reservations`. |
| Failing order creation | Both writes are rolled back. |
| Listing orders | Runs through a read-only transaction. |
| Confirming an order | Uses explicit transaction options. |
| Rebuilding projections | Runs a multi-item `Flux` workflow inside one transaction boundary. |

## CI

This demo is a standalone Gradle project under `examples/spring-boot-webflux-r2dbc-ddd-demo`.
CI should run it separately from the root build:

```yaml
- name: Test WebFlux R2DBC DDD demo
  working-directory: examples/spring-boot-webflux-r2dbc-ddd-demo
  run: ./gradlew clean test --refresh-dependencies
```

Docker must be available because tests use Testcontainers with PostgreSQL.

## Notes

IntelliJ IDEA may navigate to local source files because the demo lives inside the same repository.
Gradle dependency resolution is the source of truth.

If a fresh snapshot was published, always run consumer tests with:

```bash
./gradlew clean test --refresh-dependencies
```
