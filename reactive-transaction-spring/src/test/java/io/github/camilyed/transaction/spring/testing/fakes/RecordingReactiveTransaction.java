package io.github.camilyed.transaction.spring.testing.fakes;

import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.TransactionDefinition;

public final class RecordingReactiveTransaction implements ReactiveTransaction {

  private final TransactionDefinition definition;
  private boolean rollbackOnly;
  private boolean completed;

  RecordingReactiveTransaction(TransactionDefinition definition) {
    this.definition = definition;
  }

  @Override
  public @org.jspecify.annotations.Nullable String getTransactionName() {
    return definition.getName();
  }

  @Override
  public boolean isReadOnly() {
    return definition.isReadOnly();
  }

  @Override
  public void setRollbackOnly() {
    this.rollbackOnly = true;
  }

  @Override
  public boolean isRollbackOnly() {
    return rollbackOnly;
  }

  @Override
  public boolean isCompleted() {
    return completed;
  }

  void complete() {
    this.completed = true;
  }
}
