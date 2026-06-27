# Spring Boot WebFlux R2DBC DDD Demo

This standalone demo application shows how to use `reactive-transaction-spring-boot-starter` from a remote Sonatype Central snapshot in a Spring Boot WebFlux + R2DBC application.

The demo intentionally consumes the published snapshot instead of depending on local Gradle project modules.

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

## What this demo proves

The application demonstrates that the starter can auto-configure `ReactiveTransaction` when a Spring `ReactiveTransactionManager` is available in the application context.

The demo covers:

- Spring Boot WebFlux with R2DBC
- PostgreSQL as the real database
- Testcontainers-based integration tests
- DDD-style application services
- Hexagonal architecture with ports and adapters
- `ReactiveTransaction.inTransaction(...)` for `Mono`
- `ReactiveTransaction.inTransactionMany(...)` for `Flux`
- commit behavior
- rollback behavior
- read-only transaction options
- `REQUIRES_NEW` propagation
- `SERIALIZABLE` isolation
- transaction timeout configuration

## Architecture

The demo keeps transaction boundaries in the application layer.

Application services depend on the library abstraction:

```java
import io.github.camilyed.transaction.ReactiveTransaction;
```

They do not depend directly on Spring's `TransactionalOperator`.

The Spring adapter is provided by the starter. The application provides standard database infrastructure, including an R2DBC `ConnectionFactory` and a Spring `R2dbcTransactionManager`.

```text
HTTP API
  -> application use case
    -> ReactiveTransaction
      -> SpringReactiveTransaction
        -> ReactiveTransactionManager
          -> R2DBC / PostgreSQL
```

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
    └── domain
```

## Transaction manager

The demo defines a standard Spring `ReactiveTransactionManager` for the PostgreSQL `ConnectionFactory`.

```java
@Configuration
class R2dbcTransactionManagerConfiguration {

  @Bean
  ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }
}
```

The library starter then auto-configures `ReactiveTransaction` on top of this Spring transaction manager.

The demo does **not** create a `ReactiveTransaction` bean manually. That bean must come from the starter.

## Running tests

The integration tests use PostgreSQL through Testcontainers.

```bash
./gradlew clean test --refresh-dependencies
```

To run only the auto-configuration smoke test:

```bash
./gradlew clean test --tests "*AutoConfigurationSmokeTest" --stacktrace
```

## Running the application locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the app:

```bash
./gradlew bootRun
```

The app starts on port `8080`.

## API examples

Create an order:

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-1","amount":49.99}'
```

List orders:

```bash
curl http://localhost:8080/orders
```

Create an order and force rollback:

```bash
curl -X POST http://localhost:8080/orders/failing \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-failing","amount":19.99}'
```

Confirm an order:

```bash
curl -X POST http://localhost:8080/orders/{orderId}/confirm
```

Rebuild projections using a `Flux` transaction:

```bash
curl -X POST http://localhost:8080/orders/projections/rebuild
```

List projections:

```bash
curl http://localhost:8080/orders/projections
```

## Expected behavior

A successful order creation writes both:

- one row to `demo_orders`
- one row to `payment_reservations`

The failing endpoint writes inside a transaction and then throws an error. Both writes are rolled back.

Projection rebuild runs as a multi-item reactive flow and demonstrates `Flux` transaction support through `inTransactionMany(...)`.

## CI

This demo is a standalone Gradle project under `examples/spring-boot-webflux-r2dbc-ddd-demo`, so CI should run its tests separately from the root build.

Recommended GitHub Actions step:

```yaml
- name: Test WebFlux R2DBC DDD demo
  working-directory: examples/spring-boot-webflux-r2dbc-ddd-demo
  run: ./gradlew clean test --refresh-dependencies
```

This step requires Docker because the tests use Testcontainers with PostgreSQL.

## Notes

The demo consumes `0.1.0-SNAPSHOT` from Sonatype Central snapshots. If a fresh snapshot was published, use `--refresh-dependencies` to force Gradle to resolve the newest timestamped snapshot.

IntelliJ IDEA may navigate to local source files when the demo lives inside the same repository. Gradle dependency resolution is the source of truth. Verify it with:

```bash
./gradlew dependencyInsight \
  --dependency reactive-transaction-spring-boot-starter \
  --configuration runtimeClasspath
```

The expected result should show `io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT`, not a local `project :...` dependency.
