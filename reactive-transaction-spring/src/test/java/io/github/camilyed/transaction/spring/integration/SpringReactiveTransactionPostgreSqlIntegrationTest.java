package io.github.camilyed.transaction.spring.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.transaction.Isolation;
import io.github.camilyed.transaction.Propagation;
import io.github.camilyed.transaction.TransactionOptions;
import java.time.Duration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SpringReactiveTransactionPostgreSqlIntegrationTest
    extends PostgreSqlR2dbcIntegrationTestSupport {

  @Test
  void shouldCommitMonoTransactionInPostgreSql() {
    // when
    var result = transaction.inTransaction(() -> insertItem("committed").thenReturn("result"));

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    StepVerifier.create(countItems()).expectNext(1L).verifyComplete();
    StepVerifier.create(findItemNames()).expectNext("committed").verifyComplete();
  }

  @Test
  void shouldRollbackMonoTransactionWhenOperationFailsInPostgreSql() {
    // given
    var failure = new IllegalStateException("operation failed");

    // when
    var result =
        transaction.inTransaction(() -> insertItem("rolled-back").then(Mono.error(failure)));

    // then
    StepVerifier.create(result)
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    StepVerifier.create(countItems()).expectNext(0L).verifyComplete();
  }

  @Test
  void shouldCommitFluxTransactionInPostgreSql() {
    // when
    var result =
        transaction.inTransactionMany(
            () -> Flux.just("one", "two").concatMap(name -> insertItem(name).thenReturn(name)));

    // then
    StepVerifier.create(result).expectNext("one", "two").verifyComplete();

    StepVerifier.create(countItems()).expectNext(2L).verifyComplete();
    StepVerifier.create(findItemNames()).expectNext("one", "two").verifyComplete();
  }

  @Test
  void shouldRollbackFluxTransactionWhenOperationFailsInPostgreSql() {
    // given
    var failure = new IllegalStateException("operation failed");

    // when
    var result =
        transaction.inTransactionMany(
            () ->
                Flux.just("one", "two")
                    .concatMap(name -> insertItem(name).thenReturn(name))
                    .concatWith(Mono.error(failure)));

    // then
    StepVerifier.create(result)
        .expectNext("one", "two")
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    StepVerifier.create(countItems()).expectNext(0L).verifyComplete();
  }

  @Test
  void shouldExecuteMonoTransactionWithOptionsInPostgreSql() {
    // given
    var options =
        TransactionOptions.defaults()
            .withIsolation(Isolation.SERIALIZABLE)
            .withPropagation(Propagation.REQUIRES_NEW)
            .withTimeout(Duration.ofSeconds(5));

    // when
    var result =
        transaction.inTransaction(options, () -> insertItem("with-options").thenReturn(1L));

    // then
    StepVerifier.create(result).expectNext(1L).verifyComplete();

    StepVerifier.create(countItems()).expectNext(1L).verifyComplete();
    StepVerifier.create(findItemNames()).expectNext("with-options").verifyComplete();
  }

  @Test
  void shouldStartReadOnlyTransactionInPostgreSql() {
    // given
    var options = TransactionOptions.defaults().withReadOnly();

    // when
    var result = transaction.inTransaction(options, this::currentTransactionReadOnly);

    // then
    StepVerifier.create(result).expectNext("on").verifyComplete();
  }

  @Test
  void shouldFailWriteInsideReadOnlyTransactionInPostgreSql() {
    // given
    var options = TransactionOptions.defaults().withReadOnly();

    // when
    var result = transaction.inTransaction(options, () -> insertItem("read-only-write"));

    // then
    StepVerifier.create(result)
        .expectErrorMatches(error -> error.getMessage().contains("read-only transaction"))
        .verify();

    StepVerifier.create(countItems()).expectNext(0L).verifyComplete();
  }

  @Test
  @Disabled("R2DBC/PostgreSQL transaction timeout behavior requires separate investigation")
  void shouldTimeoutLongRunningTransactionInPostgreSql() {
    // given
    var options = TransactionOptions.defaults().withTimeout(Duration.ofSeconds(1));

    // when
    var result =
        transaction.inTransaction(
            options, () -> sleep(Duration.ofSeconds(2)).thenReturn("completed"));

    // then
    StepVerifier.create(result).expectError().verify();

    StepVerifier.create(countItems()).expectNext(0L).verifyComplete();
  }

  @Test
  void shouldExposePostgreSqlTransactionSettings() {
    // given
    var options =
        TransactionOptions.defaults()
            .withIsolation(Isolation.SERIALIZABLE)
            .withReadOnly()
            .withTimeout(Duration.ofSeconds(5));

    // when
    var result = transaction.inTransaction(options, this::currentTransactionSettings);

    // then
    StepVerifier.create(result)
        .assertNext(
            settings -> {
              assertThat(settings.readOnly()).isEqualTo("on");
              assertThat(settings.isolation()).isEqualTo("serializable");
              assertThat(settings.statementTimeout()).isEqualTo("0");
              assertThat(settings.lockTimeout()).isEqualTo("0");
              assertThat(settings.idleTimeout()).isEqualTo("0");
            })
        .verifyComplete();
  }

  @Test
  void shouldApplyReadOnlyAndIsolationToPostgreSqlTransaction() {
    // given
    var options =
        TransactionOptions.defaults().withIsolation(Isolation.SERIALIZABLE).withReadOnly();

    // when
    var result = transaction.inTransaction(options, this::currentTransactionSettings);

    // then
    StepVerifier.create(result)
        .assertNext(
            settings -> {
              assertThat(settings.readOnly()).isEqualTo("on");
              assertThat(settings.isolation()).isEqualTo("serializable");
            })
        .verifyComplete();
  }

  @Test
  void shouldNotApplyTransactionTimeoutAsPostgreSqlStatementTimeout() {
    // given
    var options = TransactionOptions.defaults().withTimeout(Duration.ofSeconds(5));

    // when
    var result = transaction.inTransaction(options, this::currentTransactionSettings);

    // then
    StepVerifier.create(result)
        .assertNext(settings -> assertThat(settings.statementTimeout()).isEqualTo("0"))
        .verifyComplete();
  }

  @Test
  void shouldCommitRequiresNewTransactionEvenWhenOuterTransactionRollsBackInPostgreSql() {
    // given
    var outerFailure = new IllegalStateException("outer failed");
    var requiresNew = TransactionOptions.defaults().withPropagation(Propagation.REQUIRES_NEW);

    // when
    var result =
        transaction.inTransaction(
            () ->
                insertItem("outer")
                    .then(
                        transaction.inTransaction(
                            requiresNew, () -> insertItem("inner").thenReturn("inner")))
                    .then(Mono.error(outerFailure)));

    // then
    StepVerifier.create(result)
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(outerFailure))
        .verify();

    StepVerifier.create(findItemNames()).expectNext("inner").verifyComplete();
  }

  @Test
  void shouldRollbackRequiredInnerTransactionWhenOuterTransactionRollsBackInPostgreSql() {
    // given
    var outerFailure = new IllegalStateException("outer failed");
    var required = TransactionOptions.defaults().withPropagation(Propagation.REQUIRED);

    // when
    var result =
        transaction.inTransaction(
            () ->
                insertItem("outer")
                    .then(
                        transaction.inTransaction(
                            required, () -> insertItem("inner").thenReturn("inner")))
                    .then(Mono.error(outerFailure)));

    // then
    StepVerifier.create(result)
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(outerFailure))
        .verify();

    StepVerifier.create(countItems()).expectNext(0L).verifyComplete();
  }
}
