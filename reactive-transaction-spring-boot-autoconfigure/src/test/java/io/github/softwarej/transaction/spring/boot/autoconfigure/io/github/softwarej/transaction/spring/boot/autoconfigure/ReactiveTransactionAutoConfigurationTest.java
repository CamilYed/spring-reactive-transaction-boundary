package io.github.softwarej.transaction.spring.boot.autoconfigure.io.github.softwarej.transaction.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.softwarej.transaction.ReactiveTransaction;
import io.github.softwarej.transaction.TransactionOptions;
import io.github.softwarej.transaction.spring.SpringReactiveTransaction;
import io.github.softwarej.transaction.spring.boot.autoconfigure.ReactiveTransactionAutoConfiguration;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ReactiveTransactionAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ReactiveTransactionAutoConfiguration.class));

  @Test
  void shouldCreateAutoConfigurationInstance() {
    // when
    var autoConfiguration = new ReactiveTransactionAutoConfiguration();

    // then
    assertThat(autoConfiguration).isNotNull();
  }

  @Test
  void shouldCreateReactiveTransactionBeanWhenReactiveTransactionManagerIsAvailable() {
    // when / then
    contextRunner
        .withBean(ReactiveTransactionManager.class, StubReactiveTransactionManager::new)
        .run(
            context -> {
              assertThat(context).hasSingleBean(ReactiveTransaction.class);
              assertThat(context.getBean(ReactiveTransaction.class))
                  .isInstanceOf(SpringReactiveTransaction.class);
            });
  }

  @Test
  void shouldBackOffWhenReactiveTransactionBeanAlreadyExists() {
    // when / then
    contextRunner
        .withBean(ReactiveTransactionManager.class, StubReactiveTransactionManager::new)
        .withBean(ReactiveTransaction.class, UserDefinedReactiveTransaction::new)
        .run(
            context -> {
              assertThat(context).hasSingleBean(ReactiveTransaction.class);
              assertThat(context.getBean(ReactiveTransaction.class))
                  .isInstanceOf(UserDefinedReactiveTransaction.class);
            });
  }

  @Test
  void shouldNotCreateReactiveTransactionBeanWithoutReactiveTransactionManager() {
    // when / then
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(ReactiveTransaction.class));
  }

  private static final class StubReactiveTransactionManager implements ReactiveTransactionManager {

    @Override
    public @NonNull Mono<org.springframework.transaction.ReactiveTransaction>
        getReactiveTransaction(@Nullable TransactionDefinition definition) {
      return Mono.error(new UnsupportedOperationException("not used"));
    }

    @Override
    public @NonNull Mono<Void> commit(
        @NonNull org.springframework.transaction.ReactiveTransaction transaction) {
      return Mono.error(new UnsupportedOperationException("not used"));
    }

    @Override
    public @NonNull Mono<Void> rollback(
        @NonNull org.springframework.transaction.ReactiveTransaction transaction) {
      return Mono.error(new UnsupportedOperationException("not used"));
    }
  }

  private static final class UserDefinedReactiveTransaction implements ReactiveTransaction {

    @Override
    public <T> Mono<T> inTransaction(TransactionOptions options, Supplier<Mono<T>> operation) {
      return operation.get();
    }

    @Override
    public <T> Flux<T> inTransactionMany(TransactionOptions options, Supplier<Flux<T>> operation) {
      return operation.get();
    }
  }
}
