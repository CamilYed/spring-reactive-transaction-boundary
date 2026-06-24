package io.github.camilyed.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TransactionOptionsTest {

  @Test
  void shouldCreateDefaultOptions() {
    // when
    var options = TransactionOptions.defaults();

    // then
    assertThat(options.isolation()).isEqualTo(Isolation.DEFAULT);
    assertThat(options.propagation()).isEqualTo(Propagation.REQUIRED);
    assertThat(options.readOnly()).isFalse();
    assertThat(options.timeout().isDefault()).isTrue();
  }

  @Test
  void shouldCreateSerializableNewTransactionOptions() {
    // given

    // when
    var options = TransactionOptions.serializableNewTransaction();

    // then
    assertThat(options.isolation()).isEqualTo(Isolation.SERIALIZABLE);
    assertThat(options.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    assertThat(options.readOnly()).isFalse();
    assertThat(options.timeout().isDefault()).isTrue();
  }

  @Test
  void shouldChangeIsolationWithoutMutatingOriginalOptions() {
    // given
    var originalOptions = TransactionOptions.defaults();

    // when
    var changedOptions = originalOptions.withIsolation(Isolation.SERIALIZABLE);

    // then
    assertThat(originalOptions.isolation()).isEqualTo(Isolation.DEFAULT);
    assertThat(changedOptions.isolation()).isEqualTo(Isolation.SERIALIZABLE);
  }

  @Test
  void shouldChangePropagationWithoutMutatingOriginalOptions() {
    // given
    var originalOptions = TransactionOptions.defaults();

    // when
    var changedOptions = originalOptions.withPropagation(Propagation.REQUIRES_NEW);

    // then
    assertThat(originalOptions.propagation()).isEqualTo(Propagation.REQUIRED);
    assertThat(changedOptions.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
  }

  @Test
  void shouldChangeReadOnlyWithoutMutatingOriginalOptions() {
    // given
    var originalOptions = TransactionOptions.defaults();

    // when
    var changedOptions = originalOptions.withReadOnly();

    // then
    assertThat(originalOptions.readOnly()).isFalse();
    assertThat(changedOptions.readOnly()).isTrue();
  }

  @Test
  void shouldSetReadOnlyFlagExplicitly() {
    // given
    var originalOptions = TransactionOptions.defaults().withReadOnly();

    // when
    var changedOptions = originalOptions.withReadOnly(false);

    // then
    assertThat(originalOptions.readOnly()).isTrue();
    assertThat(changedOptions.readOnly()).isFalse();
  }

  @Test
  void shouldChangeToReadWrite() {
    // given
    var readOnlyOptions = TransactionOptions.defaults().withReadOnly();

    // when
    var readWriteOptions = readOnlyOptions.withReadWrite();

    // then
    assertThat(readOnlyOptions.readOnly()).isTrue();
    assertThat(readWriteOptions.readOnly()).isFalse();
  }

  @Test
  void shouldSetFixedTimeoutUsingDuration() {
    // given
    var originalOptions = TransactionOptions.defaults();
    var timeout = Duration.ofSeconds(10);

    // when
    var changedOptions = originalOptions.withTimeout(timeout);

    // then
    assertThat(originalOptions.timeout().isDefault()).isTrue();
    assertThat(changedOptions.timeout().isFixed()).isTrue();
    assertThat(changedOptions.timeout().duration()).contains(timeout);
  }

  @Test
  void shouldSetFixedTimeoutUsingTransactionTimeout() {
    // given
    var timeout = TransactionTimeout.of(Duration.ofSeconds(3));

    // when
    var options = TransactionOptions.defaults().withTimeout(timeout);

    // then
    assertThat(options.timeout()).isSameAs(timeout);
  }

  @Test
  void shouldRestoreDefaultTimeout() {
    // given
    var optionsWithTimeout = TransactionOptions.defaults().withTimeout(Duration.ofSeconds(10));

    // when
    var options = optionsWithTimeout.withoutTimeout();

    // then
    assertThat(optionsWithTimeout.timeout().isFixed()).isTrue();
    assertThat(options.timeout().isDefault()).isTrue();
  }

  @Test
  void shouldRejectNullIsolation() {
    // given
    var options = TransactionOptions.defaults();

    // when / then
    assertThatThrownBy(() -> options.withIsolation(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("isolation must not be null");
  }

  @Test
  void shouldRejectNullPropagation() {
    // given
    var options = TransactionOptions.defaults();

    // when / then
    assertThatThrownBy(() -> options.withPropagation(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("propagation must not be null");
  }

  @Test
  void shouldRejectNullTransactionTimeout() {
    // given
    var options = TransactionOptions.defaults();

    // when / then
    assertThatThrownBy(() -> options.withTimeout((TransactionTimeout) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("timeout must not be null");
  }

  @Test
  void shouldRejectNullDurationTimeout() {
    // given
    var options = TransactionOptions.defaults();

    // when / then
    assertThatThrownBy(() -> options.withTimeout((Duration) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("timeout duration must not be null");
  }

  @Test
  void shouldRejectZeroDurationTimeout() {
    // given
    var options = TransactionOptions.defaults();

    // when / then
    assertThatThrownBy(() -> options.withTimeout(Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout duration must be positive");
  }

  @Test
  void shouldRejectNegativeDurationTimeout() {
    // given
    var options = TransactionOptions.defaults();

    // when / then
    assertThatThrownBy(() -> options.withTimeout(Duration.ofMillis(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout duration must be positive");
  }
}
