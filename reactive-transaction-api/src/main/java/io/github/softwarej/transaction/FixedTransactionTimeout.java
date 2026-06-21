package io.github.softwarej.transaction;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

record FixedTransactionTimeout(Duration value) implements TransactionTimeout {

  FixedTransactionTimeout {
    Objects.requireNonNull(value, "timeout duration must not be null");

    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException("timeout duration must be positive");
    }
  }

  @Override
  public Optional<Duration> duration() {
    return Optional.of(value);
  }

  @Override
  public String toString() {
    return "TransactionTimeout[" + value + "]";
  }
}
