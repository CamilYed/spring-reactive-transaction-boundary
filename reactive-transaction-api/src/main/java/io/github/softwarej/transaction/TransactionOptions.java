package io.github.softwarej.transaction;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration describing how a transaction should be started.
 *
 * <p>Instances of this class are immutable. Configuration can be customized using the {@code
 * with...()} methods, each of which returns a new instance.
 *
 * <p>Use {@link #defaults()} to obtain the default transaction configuration.
 *
 * @since 0.1.0
 */
public final class TransactionOptions {

  private static final TransactionOptions DEFAULTS =
      new TransactionOptions(
          Isolation.DEFAULT, Propagation.REQUIRED, false, TransactionTimeout.defaultTimeout());

  private final Isolation isolation;
  private final Propagation propagation;
  private final boolean readOnly;
  private final TransactionTimeout timeout;

  private TransactionOptions(
      Isolation isolation, Propagation propagation, boolean readOnly, TransactionTimeout timeout) {
    this.isolation = Objects.requireNonNull(isolation, "isolation must not be null");
    this.propagation = Objects.requireNonNull(propagation, "propagation must not be null");
    this.readOnly = readOnly;
    this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
  }

  /**
   * Returns the default transaction configuration.
   *
   * <p>The default configuration uses {@link Isolation#DEFAULT}, {@link Propagation#REQUIRED},
   * read-write mode, and the transaction manager's default timeout.
   *
   * @return the default transaction options
   */
  public static TransactionOptions defaults() {
    return DEFAULTS;
  }

  /**
   * Returns transaction options configured for a new serializable transaction.
   *
   * <p>This is a convenience factory equivalent to starting from {@link #defaults()} and applying
   * {@link Isolation#SERIALIZABLE} together with {@link Propagation#REQUIRES_NEW}.
   *
   * @return transaction options using {@link Isolation#SERIALIZABLE} isolation and {@link
   *     Propagation#REQUIRES_NEW} propagation
   */
  public static TransactionOptions serializableNewTransaction() {
    return defaults()
        .withIsolation(Isolation.SERIALIZABLE)
        .withPropagation(Propagation.REQUIRES_NEW);
  }

  /**
   * Returns the configured transaction isolation level.
   *
   * @return the transaction isolation level
   */
  public Isolation isolation() {
    return isolation;
  }

  /**
   * Returns the configured transaction propagation behavior.
   *
   * @return the transaction propagation behavior
   */
  public Propagation propagation() {
    return propagation;
  }

  /**
   * Returns whether the transaction should be marked as read-only.
   *
   * <p>The exact behavior of a read-only transaction depends on the underlying transaction manager
   * and database driver.
   *
   * @return {@code true} if the transaction should be read-only, otherwise {@code false}
   */
  public boolean readOnly() {
    return readOnly;
  }

  /**
   * Returns the configured transaction timeout.
   *
   * @return the transaction timeout configuration
   */
  public TransactionTimeout timeout() {
    return timeout;
  }

  /**
   * Returns a copy of these options using the given isolation level.
   *
   * @param isolation the isolation level to use
   * @return a new transaction options instance
   * @throws NullPointerException if {@code isolation} is {@code null}
   */
  public TransactionOptions withIsolation(Isolation isolation) {
    return new TransactionOptions(isolation, propagation, readOnly, timeout);
  }

  /**
   * Returns a copy of these options using the given propagation behavior.
   *
   * @param propagation the propagation behavior to use
   * @return a new transaction options instance
   * @throws NullPointerException if {@code propagation} is {@code null}
   */
  public TransactionOptions withPropagation(Propagation propagation) {
    return new TransactionOptions(isolation, propagation, readOnly, timeout);
  }

  /**
   * Returns a copy of these options configured as read-only.
   *
   * @return a new transaction options instance configured as read-only
   */
  public TransactionOptions withReadOnly() {
    return withReadOnly(true);
  }

  /**
   * Returns a copy of these options using the given read-only flag.
   *
   * @param readOnly whether the transaction should be marked as read-only
   * @return a new transaction options instance
   */
  public TransactionOptions withReadOnly(boolean readOnly) {
    return new TransactionOptions(isolation, propagation, readOnly, timeout);
  }

  /**
   * Returns a copy of these options configured as read-write.
   *
   * @return a new transaction options instance configured as read-write
   */
  public TransactionOptions withReadWrite() {
    return withReadOnly(false);
  }

  /**
   * Returns a copy of these options using the given fixed timeout duration.
   *
   * @param timeout the timeout duration to use
   * @return a new transaction options instance
   * @throws NullPointerException if {@code timeout} is {@code null}
   * @throws IllegalArgumentException if {@code timeout} is zero or negative
   */
  public TransactionOptions withTimeout(Duration timeout) {
    return withTimeout(TransactionTimeout.of(timeout));
  }

  /**
   * Returns a copy of these options using the given timeout configuration.
   *
   * @param timeout the timeout configuration to use
   * @return a new transaction options instance
   * @throws NullPointerException if {@code timeout} is {@code null}
   */
  public TransactionOptions withTimeout(TransactionTimeout timeout) {
    return new TransactionOptions(isolation, propagation, readOnly, timeout);
  }

  /**
   * Returns a copy of these options using the transaction manager's default timeout.
   *
   * @return a new transaction options instance using the default timeout
   */
  public TransactionOptions withoutTimeout() {
    return withTimeout(TransactionTimeout.defaultTimeout());
  }
}
