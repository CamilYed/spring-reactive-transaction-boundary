package io.github.camilyed.transaction.spring.testing.assertions;

import org.springframework.transaction.TransactionDefinition;

public final class TransactionDefinitionAssertions {

  private TransactionDefinitionAssertions() {}

  public static TransactionDefinitionAssert assertThatTransactionDefinition(
      TransactionDefinition actual) {
    return new TransactionDefinitionAssert(actual);
  }
}
