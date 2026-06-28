package io.github.camilyed.transaction.spring.integration;

import io.github.camilyed.transaction.spring.SpringReactiveTransaction;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.TransactionDefinition;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
abstract class PostgreSqlR2dbcIntegrationTestSupport {

  private static final int POSTGRESQL_PORT = 5432;

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("transaction_boundary")
          .withUsername("transaction_user")
          .withPassword("transaction_password");

  protected DatabaseClient databaseClient;
  protected SpringReactiveTransaction transaction;

  @BeforeEach
  void setUpPostgreSqlDatabase() {
    ConnectionFactory connectionFactory = createConnectionFactory();

    this.databaseClient = DatabaseClient.create(connectionFactory);
    this.transaction =
        new SpringReactiveTransaction(
            new PostgreSqlStatementTimeoutR2dbcTransactionManager(connectionFactory));

    recreateSchema();
  }

  protected Mono<String> currentTransactionReadOnly() {
    return databaseClient
        .sql("SHOW transaction_read_only")
        .map((row, metadata) -> row.get("transaction_read_only", String.class))
        .one();
  }

  protected Mono<String> currentTransactionIsolation() {
    return databaseClient
        .sql("SHOW transaction_isolation")
        .map((row, metadata) -> row.get("transaction_isolation", String.class))
        .one();
  }

  protected Mono<Void> sleep(Duration duration) {
    return databaseClient
        .sql("SELECT pg_sleep(:seconds)")
        .bind("seconds", duration.toMillis() / 1000.0)
        .then();
  }

  protected Mono<Long> insertItem(String name) {
    return databaseClient
        .sql("INSERT INTO transaction_items(name) VALUES (:name)")
        .bind("name", name)
        .fetch()
        .rowsUpdated();
  }

  protected Mono<Long> countItems() {
    return databaseClient
        .sql("SELECT COUNT(*) AS item_count FROM transaction_items")
        .map((row, metadata) -> row.get("item_count", Long.class))
        .one();
  }

  protected Flux<String> findItemNames() {
    return databaseClient
        .sql("SELECT name FROM transaction_items ORDER BY id")
        .map((row, metadata) -> row.get("name", String.class))
        .all();
  }

  protected Mono<PostgreSqlTransactionSettings> currentTransactionSettings() {
    return databaseClient
        .sql(
            """
                    SELECT
                      current_setting('transaction_read_only') AS read_only,
                      current_setting('transaction_isolation') AS isolation,
                      (SELECT setting FROM pg_settings WHERE name = 'statement_timeout') AS statement_timeout,
                      (SELECT setting FROM pg_settings WHERE name = 'lock_timeout') AS lock_timeout,
                      (SELECT setting FROM pg_settings WHERE name = 'idle_in_transaction_session_timeout') AS idle_timeout
                    """)
        .map(
            (row, metadata) ->
                new PostgreSqlTransactionSettings(
                    row.get("read_only", String.class),
                    row.get("isolation", String.class),
                    row.get("statement_timeout", String.class),
                    row.get("lock_timeout", String.class),
                    row.get("idle_timeout", String.class)))
        .one();
  }

  record PostgreSqlTransactionSettings(
      String readOnly,
      String isolation,
      String statementTimeout,
      String lockTimeout,
      String idleTimeout) {}

  private static ConnectionFactory createConnectionFactory() {
    return new PostgresqlConnectionFactory(
        PostgresqlConnectionConfiguration.builder()
            .host(POSTGRES.getHost())
            .port(POSTGRES.getMappedPort(POSTGRESQL_PORT))
            .database(POSTGRES.getDatabaseName())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .build());
  }

  private void recreateSchema() {
    execute("DROP TABLE IF EXISTS transaction_items");

    execute(
        """
            CREATE TABLE transaction_items (
              id BIGSERIAL PRIMARY KEY,
              name VARCHAR(255) NOT NULL
            )
            """);
  }

  private void execute(String sql) {
    StepVerifier.create(databaseClient.sql(sql).fetch().rowsUpdated())
        .expectNextCount(1)
        .verifyComplete();
  }

  private static final class PostgreSqlStatementTimeoutR2dbcTransactionManager
      extends R2dbcTransactionManager {

    private PostgreSqlStatementTimeoutR2dbcTransactionManager(ConnectionFactory connectionFactory) {
      super(connectionFactory);
    }

    @Override
    protected Mono<Void> prepareTransactionalConnection(
        Connection connection, TransactionDefinition definition) {
      return super.prepareTransactionalConnection(connection, definition)
          .then(applyStatementTimeout(connection, definition));
    }

    private Mono<Void> applyStatementTimeout(
        Connection connection, TransactionDefinition definition) {
      if (definition.getTimeout() == TransactionDefinition.TIMEOUT_DEFAULT) {
        return Mono.empty();
      }

      var timeoutMillis = Math.multiplyExact(definition.getTimeout(), 1000L);

      return Mono.from(
              connection
                  .createStatement("SET LOCAL statement_timeout = " + timeoutMillis)
                  .execute())
          .flatMap(result -> Mono.from(result.getRowsUpdated()))
          .then();
    }
  }
}
