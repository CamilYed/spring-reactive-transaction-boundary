package io.github.softwarej.transaction.spring;

import io.github.softwarej.transaction.Isolation;
import io.github.softwarej.transaction.Propagation;
import io.github.softwarej.transaction.ReactiveTransaction;
import io.github.softwarej.transaction.TransactionOptions;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

/**
 * Spring-based implementation of {@link ReactiveTransaction}.
 *
 * <p>This adapter delegates transaction handling to Spring's {@link ReactiveTransactionManager} and
 * {@link TransactionalOperator}, while keeping application code dependent only on the library-level
 * {@link ReactiveTransaction} API.
 */
public final class SpringReactiveTransaction implements ReactiveTransaction {

  private final ReactiveTransactionManager transactionManager;

  /**
   * Creates a new Spring reactive transaction boundary.
   *
   * @param transactionManager the Spring reactive transaction manager to use
   * @throws NullPointerException if {@code transactionManager} is null
   */
  public SpringReactiveTransaction(ReactiveTransactionManager transactionManager) {
    this.transactionManager =
        Objects.requireNonNull(transactionManager, "transactionManager must not be null");
  }

  /**
   * Executes the supplied operation within a Spring-managed reactive transaction.
   *
   * @param options the transaction options to apply
   * @param operation the operation to execute within the transaction
   * @param <T> the result type
   * @return a {@link Mono} emitting the operation result
   * @throws NullPointerException if {@code options} or {@code operation} is null
   */
  @Override
  public <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation) {
    Objects.requireNonNull(options, "options must not be null");
    Objects.requireNonNull(operation, "operation must not be null");

    TransactionalOperator operator =
        TransactionalOperator.create(transactionManager, toSpringTransactionDefinition(options));

    return Mono.defer(operation).as(operator::transactional);
  }

  private static TransactionDefinition toSpringTransactionDefinition(TransactionOptions options) {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

    definition.setIsolationLevel(toSpringIsolation(options.isolation()));
    definition.setPropagationBehavior(toSpringPropagation(options.propagation()));
    definition.setReadOnly(options.readOnly());

    options
        .timeout()
        .duration()
        .ifPresent(duration -> definition.setTimeout(toSpringTimeoutSeconds(duration)));

    return definition;
  }

  private static int toSpringIsolation(Isolation isolation) {
    return switch (isolation) {
      case DEFAULT -> TransactionDefinition.ISOLATION_DEFAULT;
      case READ_UNCOMMITTED -> TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
      case READ_COMMITTED -> TransactionDefinition.ISOLATION_READ_COMMITTED;
      case REPEATABLE_READ -> TransactionDefinition.ISOLATION_REPEATABLE_READ;
      case SERIALIZABLE -> TransactionDefinition.ISOLATION_SERIALIZABLE;
    };
  }

  private static int toSpringPropagation(Propagation propagation) {
    return switch (propagation) {
      case REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED;
      case REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
      case SUPPORTS -> TransactionDefinition.PROPAGATION_SUPPORTS;
      case MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY;
      case NOT_SUPPORTED -> TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
      case NEVER -> TransactionDefinition.PROPAGATION_NEVER;
      case NESTED -> TransactionDefinition.PROPAGATION_NESTED;
    };
  }

  private static int toSpringTimeoutSeconds(Duration duration) {
    long seconds = duration.getSeconds();

    if (duration.getNano() > 0) {
      seconds++;
    }

    return Math.toIntExact(seconds);
  }
}
