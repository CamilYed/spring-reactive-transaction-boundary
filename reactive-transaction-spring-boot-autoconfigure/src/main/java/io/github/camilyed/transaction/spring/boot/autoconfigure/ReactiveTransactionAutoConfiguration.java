package io.github.camilyed.transaction.spring.boot.autoconfigure;

import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.spring.SpringReactiveTransaction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Auto-configuration for the reactive transaction boundary.
 *
 * <p>This auto-configuration creates a {@link ReactiveTransaction} bean when Spring's {@link
 * ReactiveTransactionManager} is available in the application context and no user-defined {@link
 * ReactiveTransaction} bean already exists.
 *
 * <p>The configured bean delegates transaction handling to Spring through {@link
 * SpringReactiveTransaction}.
 */
@AutoConfiguration
@AutoConfigureAfter(
    name = "org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration")
@ConditionalOnClass({ReactiveTransactionManager.class, SpringReactiveTransaction.class})
public class ReactiveTransactionAutoConfiguration {

  /** Creates a new reactive transaction auto-configuration. */
  public ReactiveTransactionAutoConfiguration() {
    // Empty constructor required by Spring Boot auto-configuration.
  }

  /**
   * Creates the default reactive transaction boundary.
   *
   * @param transactionManager the Spring reactive transaction manager
   * @return the application-level reactive transaction boundary
   */
  @Bean
  @ConditionalOnBean(ReactiveTransactionManager.class)
  @ConditionalOnMissingBean(ReactiveTransaction.class)
  ReactiveTransaction reactiveTransaction(ReactiveTransactionManager transactionManager) {
    return new SpringReactiveTransaction(transactionManager);
  }
}
