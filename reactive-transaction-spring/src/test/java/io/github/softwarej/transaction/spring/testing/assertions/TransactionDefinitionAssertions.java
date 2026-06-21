package io.github.softwarej.transaction.spring.testing.assertions;

import org.springframework.transaction.TransactionDefinition;

public final class TransactionDefinitionAssertions {

    private TransactionDefinitionAssertions() {}

    public static TransactionDefinitionAssert assertThatTransactionDefinition(
            TransactionDefinition actual) {
        return new TransactionDefinitionAssert(actual);
    }
}