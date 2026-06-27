package io.github.camilyed.transaction.demo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
abstract class PostgreSqlTestContainerSupport {

  private static final int POSTGRESQL_PORT = 5432;

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("transaction_demo")
          .withUsername("transaction_user")
          .withPassword("transaction_password");

  @DynamicPropertySource
  static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
    POSTGRES.start();

    registry.add("spring.r2dbc.url", PostgreSqlTestContainerSupport::r2dbcUrl);
    registry.add("spring.r2dbc.username", POSTGRES::getUsername);
    registry.add("spring.r2dbc.password", POSTGRES::getPassword);
  }

  private static String r2dbcUrl() {
    return "r2dbc:postgresql://"
        + POSTGRES.getHost()
        + ":"
        + POSTGRES.getMappedPort(POSTGRESQL_PORT)
        + "/"
        + POSTGRES.getDatabaseName();
  }
}
