package io.github.softwarej.transaction.spring.testing.assertions;

import org.assertj.core.api.AbstractAssert;
import org.springframework.transaction.TransactionDefinition;

public final class TransactionDefinitionAssert
    extends AbstractAssert<TransactionDefinitionAssert, TransactionDefinition> {

  TransactionDefinitionAssert(TransactionDefinition actual) {
    super(actual, TransactionDefinitionAssert.class);
  }

  public TransactionDefinitionAssert hasIsolationLevel(int expectedIsolationLevel) {
    isNotNull();

    if (actual.getIsolationLevel() != expectedIsolationLevel) {
      failWithMessage(
          "Expected transaction isolation level to be <%s> but was <%s>",
          expectedIsolationLevel, actual.getIsolationLevel());
    }

    return this;
  }

  public TransactionDefinitionAssert hasPropagationBehavior(int expectedPropagationBehavior) {
    isNotNull();

    if (actual.getPropagationBehavior() != expectedPropagationBehavior) {
      failWithMessage(
          "Expected transaction propagation behavior to be <%s> but was <%s>",
          expectedPropagationBehavior, actual.getPropagationBehavior());
    }

    return this;
  }

  public TransactionDefinitionAssert isReadOnly() {
    isNotNull();

    if (!actual.isReadOnly()) {
      failWithMessage("Expected transaction definition to be read-only");
    }

    return this;
  }

  public TransactionDefinitionAssert isReadWrite() {
    isNotNull();

    if (actual.isReadOnly()) {
      failWithMessage("Expected transaction definition to be read-write");
    }

    return this;
  }

  public TransactionDefinitionAssert hasTimeout(int expectedTimeout) {
    isNotNull();

    if (actual.getTimeout() != expectedTimeout) {
      failWithMessage(
          "Expected transaction timeout to be <%s> but was <%s>",
          expectedTimeout, actual.getTimeout());
    }

    return this;
  }
}
