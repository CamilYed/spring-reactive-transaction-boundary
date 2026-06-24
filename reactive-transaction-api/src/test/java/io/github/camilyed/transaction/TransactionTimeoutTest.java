package io.github.camilyed.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TransactionTimeoutTest {

  @Test
  void shouldCreateDefaultTimeout() {
    // Given / When
    var timeout = TransactionTimeout.defaultTimeout();

    // Then
    assertThat(timeout.isDefault()).isTrue();
    assertThat(timeout.isFixed()).isFalse();
    assertThat(timeout.duration()).isEmpty();
    assertThat(timeout).hasToString("TransactionTimeout.DEFAULT");
  }

  @Test
  void shouldCreateFixedTimeout() {
    // Given
    var duration = Duration.ofSeconds(5);

    // When
    var timeout = TransactionTimeout.of(duration);

    // Then
    assertThat(timeout.isDefault()).isFalse();
    assertThat(timeout.isFixed()).isTrue();
    assertThat(timeout.duration()).contains(duration);
  }

  @Test
  void shouldCreateFixedTimeoutWithMinimalPositiveDuration() {
    // Given
    var duration = Duration.ofNanos(1);

    // When
    var timeout = TransactionTimeout.of(duration);

    // Then
    assertThat(timeout.isFixed()).isTrue();
    assertThat(timeout.duration()).contains(duration);
  }

  @Test
  void shouldRenderFixedTimeoutAsString() {
    // Given
    var duration = Duration.ofSeconds(5);

    // When
    var timeout = TransactionTimeout.of(duration);

    // Then
    assertThat(timeout).hasToString("TransactionTimeout[PT5S]");
  }

  @Test
  void shouldCompareFixedTimeoutsByDuration() {
    // Given
    var duration = Duration.ofSeconds(5);

    // When
    var first = TransactionTimeout.of(duration);
    var second = TransactionTimeout.of(duration);
    var different = TransactionTimeout.of(Duration.ofSeconds(10));

    // Then
    assertThat(first).isEqualTo(second);
    assertThat(first).hasSameHashCodeAs(second);
    assertThat(first).isNotEqualTo(different);
  }

  @Test
  void shouldRejectNullDuration() {
    // Given / When / Then
    assertThatThrownBy(() -> TransactionTimeout.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("timeout duration must not be null");
  }

  @Test
  void shouldRejectZeroDuration() {
    // Given
    var duration = Duration.ZERO;

    // When / Then
    assertThatThrownBy(() -> TransactionTimeout.of(duration))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout duration must be positive");
  }

  @Test
  void shouldRejectNegativeDuration() {
    // Given
    var duration = Duration.ofSeconds(-1);

    // When / Then
    assertThatThrownBy(() -> TransactionTimeout.of(duration))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout duration must be positive");
  }
}
