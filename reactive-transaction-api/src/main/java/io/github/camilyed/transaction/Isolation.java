package io.github.camilyed.transaction;

/**
 * Supported transaction isolation levels.
 *
 * <p>The values in this enum describe the desired isolation level for a transaction. The actual
 * behavior depends on the underlying transaction manager, database driver, and database engine.
 *
 * @since 0.1.0
 */
public enum Isolation {

  /**
   * Uses the default isolation level configured by the underlying transaction manager or database.
   */
  DEFAULT,

  /**
   * Allows dirty reads, non-repeatable reads, and phantom reads.
   *
   * <p>Not all databases support this isolation level. Some databases may silently upgrade it to a
   * stricter level.
   */
  READ_UNCOMMITTED,

  /** Prevents dirty reads, while non-repeatable reads and phantom reads may still occur. */
  READ_COMMITTED,

  /**
   * Prevents dirty reads and non-repeatable reads, while phantom reads may still occur depending on
   * the database.
   */
  REPEATABLE_READ,

  /**
   * Provides the strictest standard isolation level.
   *
   * <p>Concurrent transactions should behave as if they were executed sequentially, although the
   * exact implementation depends on the database.
   */
  SERIALIZABLE
}
