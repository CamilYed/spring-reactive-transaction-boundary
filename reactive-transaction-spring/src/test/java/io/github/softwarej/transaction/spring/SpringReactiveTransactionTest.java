package io.github.softwarej.transaction.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.softwarej.transaction.TransactionOptions;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SpringReactiveTransactionTest {

  @Test
  void shouldRejectNullTransactionManager() {
    // when / then
    assertThatThrownBy(() -> new SpringReactiveTransaction(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("transactionManager must not be null");
  }

  @Test
  void shouldRejectNullOptions() {
    // given
    var transaction = new SpringReactiveTransaction(new RecordingReactiveTransactionManager());
    Supplier<Mono<String>> operation = () -> Mono.just("result");

    // when / then
    assertThatThrownBy(() -> transaction.inTransaction(null, operation))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("options must not be null");
  }

  @Test
  void shouldRejectNullOperation() {
    // given
    var transaction = new SpringReactiveTransaction(new RecordingReactiveTransactionManager());

    // when / then
    assertThatThrownBy(() -> transaction.inTransaction(TransactionOptions.defaults(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("operation must not be null");
  }

  @Test
  void shouldCreateOperationLazily() {
    // given
    var transaction = new SpringReactiveTransaction(new RecordingReactiveTransactionManager());
    var operationCreated = new AtomicBoolean(false);

    // when
    var result =
        transaction.inTransaction(
            TransactionOptions.defaults(),
            () -> {
              operationCreated.set(true);
              return Mono.just("result");
            });

    // then
    assertThat(operationCreated).isFalse();

    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThat(operationCreated).isTrue();
  }

  @Test
  void shouldReturnOperationResultAndCommitTransaction() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    // when
    var result =
        transaction.inTransaction(TransactionOptions.defaults(), () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThat(transactionManager.startedTransactions()).isEqualTo(1);
    assertThat(transactionManager.committedTransactions()).isEqualTo(1);
    assertThat(transactionManager.rolledBackTransactions()).isZero();
  }

  @Test
  void shouldPropagateOperationErrorAndRollbackTransaction() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);
    var failure = new IllegalStateException("operation failed");

    // when
    var result =
        transaction.inTransaction(TransactionOptions.defaults(), () -> Mono.error(failure));

    // then
    StepVerifier.create(result)
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    assertThat(transactionManager.startedTransactions()).isEqualTo(1);
    assertThat(transactionManager.committedTransactions()).isZero();
    assertThat(transactionManager.rolledBackTransactions()).isEqualTo(1);
  }

  private static final class RecordingReactiveTransactionManager
      implements ReactiveTransactionManager {

    private final AtomicInteger startedTransactions = new AtomicInteger();
    private final AtomicInteger committedTransactions = new AtomicInteger();
    private final AtomicInteger rolledBackTransactions = new AtomicInteger();

    @Override
    public @NonNull Mono<ReactiveTransaction> getReactiveTransaction(
        @Nullable TransactionDefinition definition) {
      return Mono.fromSupplier(
          () -> {
            startedTransactions.incrementAndGet();

            TransactionDefinition effectiveDefinition =
                definition != null ? definition : new DefaultTransactionDefinition();

            return new RecordingReactiveTransaction(effectiveDefinition);
          });
    }

    @Override
    public @NonNull Mono<Void> commit(@NonNull ReactiveTransaction transaction) {
      Objects.requireNonNull(transaction, "transaction must not be null");

      return Mono.fromRunnable(
          () -> {
            committedTransactions.incrementAndGet();
            ((RecordingReactiveTransaction) transaction).complete();
          });
    }

    @Override
    public @NonNull Mono<Void> rollback(@NonNull ReactiveTransaction transaction) {
      Objects.requireNonNull(transaction, "transaction must not be null");

      return Mono.fromRunnable(
          () -> {
            rolledBackTransactions.incrementAndGet();
            ((RecordingReactiveTransaction) transaction).complete();
          });
    }

    int startedTransactions() {
      return startedTransactions.get();
    }

    int committedTransactions() {
      return committedTransactions.get();
    }

    int rolledBackTransactions() {
      return rolledBackTransactions.get();
    }
  }

  private static final class RecordingReactiveTransaction implements ReactiveTransaction {

    private final TransactionDefinition definition;
    private boolean rollbackOnly;
    private boolean completed;

    private RecordingReactiveTransaction(TransactionDefinition definition) {
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

    private void complete() {
      this.completed = true;
    }
  }
}
