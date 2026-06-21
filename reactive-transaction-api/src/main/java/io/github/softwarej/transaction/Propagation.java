package io.github.softwarej.transaction;

/**
 * Supported transaction propagation behaviors.
 *
 * <p>The values in this enum describe how a transaction boundary should behave when an operation is
 * executed with or without an already existing transaction. The exact behavior depends on the
 * underlying transaction manager.
 *
 * @since 0.1.0
 */
public enum Propagation {

  /** Supports an existing transaction and creates a new one if none exists. */
  REQUIRED,

  /**
   * Always creates a new transaction and suspends an existing one if possible.
   *
   * <p>Whether transaction suspension is supported depends on the underlying transaction manager.
   */
  REQUIRES_NEW,

  /** Supports an existing transaction but executes without a transaction if none exists. */
  SUPPORTS,

  /**
   * Requires an existing transaction.
   *
   * <p>If no transaction exists, the underlying transaction manager is expected to reject the
   * operation.
   */
  MANDATORY,

  /**
   * Executes without a transaction and suspends an existing one if possible.
   *
   * <p>Whether transaction suspension is supported depends on the underlying transaction manager.
   */
  NOT_SUPPORTED,

  /** Executes without a transaction and rejects execution if a transaction already exists. */
  NEVER,

  /**
   * Executes within a nested transaction if supported by the underlying transaction manager.
   *
   * <p>Nested transactions are not supported by every reactive transaction manager or database
   * driver.
   */
  NESTED
}
