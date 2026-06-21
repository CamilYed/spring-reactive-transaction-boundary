package io.github.softwarej.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveTransactionTest {

  @Test
  void shouldUseDefaultOptionsWhenNoOptionsAreProvided() {
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

  private static final class CapturingReactiveTransaction implements ReactiveTransaction {

    private TransactionOptions capturedOptions;
    private Supplier<?> capturedOperation;

    @Override
    public <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation) {
      this.capturedOptions = options;
      this.capturedOperation = operation;

      return operation.get();
    }
  }
}
