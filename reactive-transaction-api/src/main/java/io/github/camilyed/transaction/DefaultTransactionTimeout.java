package io.github.camilyed.transaction;

import java.time.Duration;
import java.util.Optional;

final class DefaultTransactionTimeout implements TransactionTimeout {

  static final DefaultTransactionTimeout INSTANCE = new DefaultTransactionTimeout();

  private DefaultTransactionTimeout() {}

  @Override
  public Optional<Duration> duration() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "TransactionTimeout.DEFAULT";
  }
}
