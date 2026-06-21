package io.github.softwarej.transaction;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents transaction timeout configuration.
 *
 * <p>A transaction timeout may either use the default timeout of the underlying transaction manager
 * or specify an explicit fixed duration.
 *
 * <p>This type is used instead of {@code null} to make the absence of an explicit timeout part of
 * the public API.
 *
 * @since 0.1.0
 */
public sealed interface TransactionTimeout
    permits DefaultTransactionTimeout, FixedTransactionTimeout {

  /**
   * Returns a timeout configuration that uses the default timeout of the underlying transaction
   * manager.
   *
   * @return the default transaction timeout configuration
   */
  static TransactionTimeout defaultTimeout() {
    return DefaultTransactionTimeout.INSTANCE;
  }

  /**
   * Creates a timeout configuration with the given fixed duration.
   *
   * @param duration the fixed timeout duration
   * @return a fixed transaction timeout configuration
   * @throws NullPointerException if {@code duration} is {@code null}
   * @throws IllegalArgumentException if {@code duration} is zero or negative
   */
  static TransactionTimeout of(Duration duration) {
    return new FixedTransactionTimeout(duration);
  }

  /**
   * Returns the fixed timeout duration, if one has been configured.
   *
   * @return an {@link Optional} containing the fixed duration, or an empty {@link Optional} if the
   *     default timeout should be used
   */
  Optional<Duration> duration();

  /**
   * Returns whether this timeout uses the default timeout of the underlying transaction manager.
   *
   * @return {@code true} if this timeout is default-based, otherwise {@code false}
   */
  default boolean isDefault() {
    return duration().isEmpty();
  }

  /**
   * Returns whether this timeout uses an explicit fixed duration.
   *
   * @return {@code true} if this timeout has a fixed duration, otherwise {@code false}
   */
  default boolean isFixed() {
    return duration().isPresent();
  }
}
