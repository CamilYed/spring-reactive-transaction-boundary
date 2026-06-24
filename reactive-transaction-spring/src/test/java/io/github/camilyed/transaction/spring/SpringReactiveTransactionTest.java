package io.github.camilyed.transaction.spring;

import static io.github.camilyed.transaction.spring.testing.assertions.TransactionDefinitionAssertions.assertThatTransactionDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.camilyed.transaction.Isolation;
import io.github.camilyed.transaction.Propagation;
import io.github.camilyed.transaction.TransactionOptions;
import io.github.camilyed.transaction.spring.testing.fakes.RecordingReactiveTransactionManager;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.TransactionDefinition;
import reactor.core.publisher.Flux;
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

  @Test
  void shouldRejectNullOptionsForManyOperation() {
    // given
    var transaction = new SpringReactiveTransaction(new RecordingReactiveTransactionManager());
    Supplier<Flux<String>> operation = () -> Flux.just("one", "two");

    // when / then
    assertThatThrownBy(() -> transaction.inTransactionMany(null, operation))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("options must not be null");
  }

  @Test
  void shouldRejectNullManyOperation() {
    // given
    var transaction = new SpringReactiveTransaction(new RecordingReactiveTransactionManager());

    // when / then
    assertThatThrownBy(() -> transaction.inTransactionMany(TransactionOptions.defaults(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("operation must not be null");
  }

  @Test
  void shouldCreateManyOperationLazily() {
    // given
    var transaction = new SpringReactiveTransaction(new RecordingReactiveTransactionManager());
    var operationCreated = new AtomicBoolean(false);

    // when
    var result =
        transaction.inTransactionMany(
            TransactionOptions.defaults(),
            () -> {
              operationCreated.set(true);
              return Flux.just("one", "two");
            });

    // then
    assertThat(operationCreated).isFalse();

    StepVerifier.create(result).expectNext("one", "two").verifyComplete();

    assertThat(operationCreated).isTrue();
  }

  @Test
  void shouldReturnManyOperationResultsAndCommitTransaction() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    // when
    var result =
        transaction.inTransactionMany(TransactionOptions.defaults(), () -> Flux.just("one", "two"));

    // then
    StepVerifier.create(result).expectNext("one", "two").verifyComplete();

    assertThat(transactionManager.startedTransactions()).isEqualTo(1);
    assertThat(transactionManager.committedTransactions()).isEqualTo(1);
    assertThat(transactionManager.rolledBackTransactions()).isZero();
  }

  @Test
  void shouldPropagateManyOperationErrorAndRollbackTransaction() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);
    var failure = new IllegalStateException("operation failed");

    // when
    var result =
        transaction.inTransactionMany(
            TransactionOptions.defaults(),
            () -> Flux.concat(Flux.just("one"), Flux.<String>error(failure)));

    // then
    StepVerifier.create(result)
        .expectNext("one")
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
    var result =
        transaction.inTransaction(TransactionOptions.defaults(), () -> Mono.just("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
        .hasIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT)
        .hasPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED)
        .isReadWrite()
        .hasTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
  }

  @Test
  void shouldApplyDefaultTransactionOptionsToManySpringTransactionDefinition() {
    // given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);

    // when
    var result =
        transaction.inTransactionMany(TransactionOptions.defaults(), () -> Flux.just("one", "two"));

    // then
    StepVerifier.create(result).expectNext("one", "two").verifyComplete();

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
  void shouldApplyTransactionOptionsToManySpringTransactionDefinition() {
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
    var result = transaction.inTransactionMany(options, () -> Flux.just("one", "two"));

    // then
    StepVerifier.create(result).expectNext("one", "two").verifyComplete();

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

  @ParameterizedTest
  @MethodSource("isolationMappings")
  void shouldMapIsolationToSpringTransactionDefinition(
      Isolation isolation, int expectedSpringIsolation) {
    // Given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);
    var options = TransactionOptions.defaults().withIsolation(isolation);

    // When
    var result = transaction.inTransaction(options, () -> Mono.just("ok"));

    // Then
    StepVerifier.create(result).expectNext("ok").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
        .hasIsolationLevel(expectedSpringIsolation);
  }

  @ParameterizedTest
  @MethodSource("propagationMappings")
  void shouldMapPropagationToSpringTransactionDefinition(
      Propagation propagation, int expectedSpringPropagation) {
    // Given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);
    var options = TransactionOptions.defaults().withPropagation(propagation);

    // When
    var result = transaction.inTransaction(options, () -> Mono.just("ok"));

    // Then
    StepVerifier.create(result).expectNext("ok").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
        .hasPropagationBehavior(expectedSpringPropagation);
  }

  @ParameterizedTest
  @MethodSource("timeoutMappings")
  void shouldMapTimeoutToSpringTransactionDefinition(
      Duration timeout, int expectedSpringTimeoutSeconds) {
    // Given
    var transactionManager = new RecordingReactiveTransactionManager();
    var transaction = new SpringReactiveTransaction(transactionManager);
    var options = TransactionOptions.defaults().withTimeout(timeout);

    // When
    var result = transaction.inTransaction(options, () -> Mono.just("ok"));

    // Then
    StepVerifier.create(result).expectNext("ok").verifyComplete();

    assertThatTransactionDefinition(transactionManager.lastTransactionDefinition())
        .hasTimeout(expectedSpringTimeoutSeconds);
  }

  private static Stream<Arguments> isolationMappings() {
    return Stream.of(
        Arguments.of(Isolation.DEFAULT, TransactionDefinition.ISOLATION_DEFAULT),
        Arguments.of(Isolation.READ_UNCOMMITTED, TransactionDefinition.ISOLATION_READ_UNCOMMITTED),
        Arguments.of(Isolation.READ_COMMITTED, TransactionDefinition.ISOLATION_READ_COMMITTED),
        Arguments.of(Isolation.REPEATABLE_READ, TransactionDefinition.ISOLATION_REPEATABLE_READ),
        Arguments.of(Isolation.SERIALIZABLE, TransactionDefinition.ISOLATION_SERIALIZABLE));
  }

  private static Stream<Arguments> propagationMappings() {
    return Stream.of(
        Arguments.of(Propagation.REQUIRED, TransactionDefinition.PROPAGATION_REQUIRED),
        Arguments.of(Propagation.REQUIRES_NEW, TransactionDefinition.PROPAGATION_REQUIRES_NEW),
        Arguments.of(Propagation.SUPPORTS, TransactionDefinition.PROPAGATION_SUPPORTS),
        Arguments.of(Propagation.MANDATORY, TransactionDefinition.PROPAGATION_MANDATORY),
        Arguments.of(Propagation.NOT_SUPPORTED, TransactionDefinition.PROPAGATION_NOT_SUPPORTED),
        Arguments.of(Propagation.NEVER, TransactionDefinition.PROPAGATION_NEVER),
        Arguments.of(Propagation.NESTED, TransactionDefinition.PROPAGATION_NESTED));
  }

  private static Stream<Arguments> timeoutMappings() {
    return Stream.of(
        Arguments.of(Duration.ofNanos(1), 1),
        Arguments.of(Duration.ofMillis(999), 1),
        Arguments.of(Duration.ofSeconds(1), 1),
        Arguments.of(Duration.ofSeconds(1).plusNanos(1), 2),
        Arguments.of(Duration.ofSeconds(5), 5));
  }
}
