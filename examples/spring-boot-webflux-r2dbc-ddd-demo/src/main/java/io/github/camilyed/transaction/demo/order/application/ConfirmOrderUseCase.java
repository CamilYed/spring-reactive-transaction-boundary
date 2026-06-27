package io.github.camilyed.transaction.demo.order.application;

import io.github.camilyed.transaction.Isolation;
import io.github.camilyed.transaction.Propagation;
import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.TransactionOptions;
import io.github.camilyed.transaction.demo.order.application.port.OrderRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderId;
import java.time.Duration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public final class ConfirmOrderUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;

  public ConfirmOrderUseCase(ReactiveTransaction transaction, OrderRepository orderRepository) {
    this.transaction = transaction;
    this.orderRepository = orderRepository;
  }

  public Mono<Order> handle(OrderId orderId) {
    var options =
        TransactionOptions.defaults()
            .withPropagation(Propagation.REQUIRES_NEW)
            .withIsolation(Isolation.SERIALIZABLE)
            .withTimeout(Duration.ofSeconds(5));

    return transaction.inTransaction(
        options,
        () -> orderRepository.findById(orderId).flatMap(order -> orderRepository.update(order.confirm())));
  }
}
