package io.github.softwarej.transaction.spring.testing.fakes;

import io.github.softwarej.transaction.spring.SpringReactiveTransactionTest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class RecordingReactiveTransactionManager
        implements ReactiveTransactionManager {

    private final AtomicInteger startedTransactions = new AtomicInteger();
    private final AtomicInteger committedTransactions = new AtomicInteger();
    private final AtomicInteger rolledBackTransactions = new AtomicInteger();
    private final AtomicReference<TransactionDefinition> lastTransactionDefinition = new AtomicReference<>();

    @Override
    public @NonNull Mono<ReactiveTransaction> getReactiveTransaction(
            @Nullable TransactionDefinition definition) {
        return Mono.fromSupplier(
                () -> {
                    startedTransactions.incrementAndGet();

                    TransactionDefinition effectiveDefinition =
                            definition != null ? definition : new DefaultTransactionDefinition();

                    lastTransactionDefinition.set(effectiveDefinition);

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

    public int startedTransactions() {
        return startedTransactions.get();
    }

    public int committedTransactions() {
        return committedTransactions.get();
    }

    public int rolledBackTransactions() {
        return rolledBackTransactions.get();
    }

    public TransactionDefinition lastTransactionDefinition() {
        return lastTransactionDefinition.get();
    }
}
