package io.github.softwarej.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TransactionTimeoutTest {

  @Test
  void shouldCreateDefaultTimeout() {
    // when
    var timeout = TransactionTimeout.defaultTimeout();

    // then
    assertThat(timeout.isDefault()).isTrue();
    assertThat(timeout.isFixed()).isFalse();
    assertThat(timeout.duration()).isEmpty();
    assertThat(timeout).hasToString("TransactionTimeout.DEFAULT");
  }

  @Test
  void shouldCreateFixedTimeout() {
    // given
    var duration = Duration.ofSeconds(5);

    // when
    var timeout = TransactionTimeout.of(duration);

    // then
    assertThat(timeout.isDefault()).isFalse();
    assertThat(timeout.isFixed()).isTrue();
    assertThat(timeout.duration()).contains(duration);
  }

  @Test
  void shouldRejectNullDuration() {
    // when / then
    assertThatThrownBy(() -> TransactionTimeout.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("timeout duration must not be null");
  }

  @Test
  void shouldRejectZeroDuration() {
    // given
    var duration = Duration.ZERO;

    // when / then
    assertThatThrownBy(() -> TransactionTimeout.of(duration))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout duration must be positive");
  }

  @Test
  void shouldRejectNegativeDuration() {
    // given
    var duration = Duration.ofSeconds(-1);

    // when / then
    assertThatThrownBy(() -> TransactionTimeout.of(duration))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout duration must be positive");
  }
}
