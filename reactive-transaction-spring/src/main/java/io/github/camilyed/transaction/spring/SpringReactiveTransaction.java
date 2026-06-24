package io.github.camilyed.transaction.spring;

import io.github.camilyed.transaction.Isolation;
import io.github.camilyed.transaction.Propagation;
import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.TransactionOptions;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring-based implementation of {@link ReactiveTransaction}.
 *
 * <p>This adapter delegates transaction handling to Spring's {@link ReactiveTransactionManager} and
 * {@link TransactionalOperator}, while keeping application code dependent only on the library-level
 * {@link ReactiveTransaction} API.
 *
 * <p>Both single-value and multi-value reactive operations are supported. Single-value operations
 * are represented by {@link Mono}; multi-value operations are represented by {@link Flux}.
 *
 * <p>Operation publishers are created lazily using {@link Mono#defer(Supplier)} and {@link
 * Flux#defer(Supplier)} so that the reactive pipeline is assembled within the transaction boundary
 * managed by Spring.
 */
public final class SpringReactiveTransaction implements ReactiveTransaction {

  private final ReactiveTransactionManager transactionManager;

  /**
   * Creates a new Spring reactive transaction boundary.
   *
   * @param transactionManager the Spring reactive transaction manager used to create, commit, and
   *     roll back transactions
   * @throws NullPointerException if {@code transactionManager} is {@code null}
   */
  public SpringReactiveTransaction(ReactiveTransactionManager transactionManager) {
    this.transactionManager =
        Objects.requireNonNull(transactionManager, "transactionManager must not be null");
  }

  /**
   * Executes the supplied single-value operation within a Spring-managed reactive transaction.
   *
   * <p>The operation is created lazily and then wrapped with Spring's {@link
   * TransactionalOperator}. The resulting {@link Mono} participates in a transaction configured
   * from the supplied {@link TransactionOptions}.
   *
   * @param options the transaction options to apply
   * @param operation the single-value operation to execute within the transaction
   * @param <T> the result type
   * @return a {@link Mono} emitting the operation result
   * @throws NullPointerException if {@code options} or {@code operation} is {@code null}
   */
  @Override
  public <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation) {
    Objects.requireNonNull(options, "options must not be null");
    Objects.requireNonNull(operation, "operation must not be null");

    TransactionalOperator operator =
        TransactionalOperator.create(transactionManager, toSpringTransactionDefinition(options));

    return Mono.defer(operation).as(operator::transactional);
  }

  /**
   * Executes the supplied multi-value operation within a Spring-managed reactive transaction.
   *
   * <p>The operation is created lazily and then wrapped with Spring's {@link
   * TransactionalOperator}. The resulting {@link Flux} participates in a transaction configured
   * from the supplied {@link TransactionOptions}.
   *
   * @param options the transaction options to apply
   * @param operation the multi-value operation to execute within the transaction
   * @param <T> the result element type
   * @return a {@link Flux} emitting the operation results
   * @throws NullPointerException if {@code options} or {@code operation} is {@code null}
   */
  @Override
  public <T> Flux<T> inTransactionMany(TransactionOptions options, Supplier<Flux<T>> operation) {
    Objects.requireNonNull(options, "options must not be null");
    Objects.requireNonNull(operation, "operation must not be null");

    TransactionalOperator operator =
        TransactionalOperator.create(transactionManager, toSpringTransactionDefinition(options));

    return Flux.defer(operation).as(operator::transactional);
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
