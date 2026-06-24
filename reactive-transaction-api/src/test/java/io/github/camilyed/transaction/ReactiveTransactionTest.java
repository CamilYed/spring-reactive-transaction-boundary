package io.github.camilyed.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveTransactionTest {

  @Test
  void shouldUseDefaultOptionsWhenNoOptionsAreProvidedForSingleValueOperation() {
    // given
    var transaction = new CapturingReactiveTransaction();
    Supplier<Mono<String>> operation = () -> Mono.just("result");

    // when
    var result = transaction.inTransaction(operation);

    // then
    StepVerifier.create(result).expectNext("result").verifyComplete();

    assertThat(transaction.capturedOptions).isSameAs(TransactionOptions.defaults());
    assertThat(transaction.capturedOperation).isSameAs(operation);
  }

  @Test
  void shouldUseDefaultOptionsWhenNoOptionsAreProvidedForMultiValueOperation() {
    // given
    var transaction = new CapturingReactiveTransaction();
    Supplier<Flux<String>> operation = () -> Flux.just("one", "two");

    // when
    var result = transaction.inTransactionMany(operation);

    // then
    StepVerifier.create(result).expectNext("one", "two").verifyComplete();

    assertThat(transaction.capturedOptions).isSameAs(TransactionOptions.defaults());
    assertThat(transaction.capturedOperation).isSameAs(operation);
  }

  private static final class CapturingReactiveTransaction implements ReactiveTransaction {

    private TransactionOptions capturedOptions;
    private Supplier<?> capturedOperation;

    @Override
    public <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation) {
      this.capturedOptions = options;
      this.capturedOperation = operation;

      return operation.get();
    }

    @Override
    public <T> Flux<T> inTransactionMany(TransactionOptions options, Supplier<Flux<T>> operation) {
      this.capturedOptions = options;
      this.capturedOperation = operation;

      return operation.get();
    }
  }
}
