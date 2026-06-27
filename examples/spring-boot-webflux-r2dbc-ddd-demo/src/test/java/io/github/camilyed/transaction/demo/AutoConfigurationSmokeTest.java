package io.github.camilyed.transaction.demo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.transaction.ReactiveTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.ReactiveTransactionManager;

@SpringBootTest
class AutoConfigurationSmokeTest extends PostgreSqlTestContainerSupport {

  @Autowired
  private ReactiveTransaction reactiveTransaction;

  @Autowired
  private ReactiveTransactionManager transactionManager;

  @Test
  void shouldLoadReactiveTransactionAutoConfigurationWithPostgreSql() {
    // expect
    assertThat(transactionManager).isNotNull();
    assertThat(reactiveTransaction).isNotNull();
  }
}
