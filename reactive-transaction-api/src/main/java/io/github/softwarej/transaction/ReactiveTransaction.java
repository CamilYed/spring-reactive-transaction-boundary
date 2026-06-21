package io.github.softwarej.transaction;

import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * Defines a reactive transaction boundary for application code.
 *
 * <p>This interface is intended to be used by application services and use cases that need to
 * execute a reactive operation within a transaction without depending directly on a specific
 * transaction infrastructure such as Spring's {@code TransactionalOperator}.
 *
 * <p>The operation is provided as a {@link Supplier} to ensure that the reactive chain is created
 * lazily by the transaction implementation. This is important because the transaction boundary
 * should be established before the operation publisher is created and subscribed to.
 */
public interface ReactiveTransaction {

  /**
   * Executes the supplied operation within a transaction using default transaction options.
   *
   * @param operation the operation to execute within the transaction
   * @param <T> the result type
   * @return a {@link Mono} emitting the operation result
   */
  default <T> Mono<T> inTransaction(Supplier<Mono<T>> operation) {
    return inTransaction(TransactionOptions.defaults(), operation);
  }

  /**
   * Executes the supplied operation within a transaction using the given transaction options.
   *
   * @param options the transaction options to apply
   * @param operation the operation to execute within the transaction
   * @param <T> the result type
   * @return a {@link Mono} emitting the operation result
   */
  <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation);
}
