package io.github.softwarej.transaction.spring;

import static io.github.softwarej.transaction.spring.testing.assertions.TransactionDefinitionAssertions.assertThatTransactionDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.softwarej.transaction.Isolation;
import io.github.softwarej.transaction.Propagation;
import io.github.softwarej.transaction.TransactionOptions;
import io.github.softwarej.transaction.spring.testing.fakes.RecordingReactiveTransactionManager;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
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
    var result = transaction.inTransaction(TransactionOptions.defaults(), () -> Mono.just("result"));

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
    var result = transaction.inTransaction(TransactionOptions.defaults(), () -> Mono.error(failure));

    // then
    StepVerifier.create(result)
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
            .verify();

    assertThat(transactionManager.startedTransactions()).isEqualTo(1);
    assertThat(transactionManager.committedTransactions()).isZero();
    assertThat(transactionManager.rolledBackTransactions()).isEqualTo(1);
  }

  @Test
  void shouldApplyDefaultTransactionOptionsToSpringTransactionDefinition() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    // when
    var result = transaction.inTransaction(TransactionOptions.defaults(), () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
            .hasIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT)
            .hasPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED)
            .isReadWrite()
            .hasTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
  }

  @Test
  void shouldApplyTransactionOptionsToSpringTransactionDefinition() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    var options =
            TransactionOptions.defaults()
                    .withIsolation(Isolation.SERIALIZABLE)
                    .withPropagation(Propagation.REQUIRES_NEW)
                    .withReadOnly()
                    .withTimeout(Duration.ofSeconds(5));

    // when
    var result = transaction.inTransaction(options, () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
            .hasIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE)
            .hasPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
            .isReadOnly()
            .hasTimeout(5);
  }

  @Test
  void shouldRoundTimeoutUpToFullSeconds() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    var options = TransactionOptions.defaults().withTimeout(Duration.ofMillis(1500));

    // when
    var result = transaction.inTransaction(options, () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition()).hasTimeout(2);
  }

  @Test
  void shouldUseExactTimeoutWhenDurationIsAlreadyFullSeconds() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    var options = TransactionOptions.defaults().withTimeout(Duration.ofSeconds(3));

    // when
    var result = transaction.inTransaction(options, () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition()).hasTimeout(3);
  }

  @Test
  void shouldUseDefaultTimeoutWhenNoTimeoutIsConfigured() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    var options = TransactionOptions.defaults().withoutTimeout();

    // when
    var result = transaction.inTransaction(options, () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
            .hasTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
  }
}