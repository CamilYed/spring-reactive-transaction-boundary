package io.github.camilyed.transaction.demo.order.application;

import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.TransactionOptions;
import io.github.camilyed.transaction.demo.order.application.port.OrderRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public final class ListOrdersUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;

  public ListOrdersUseCase(ReactiveTransaction transaction, OrderRepository orderRepository) {
    this.transaction = transaction;
    this.orderRepository = orderRepository;
  }

  public Flux<Order> handle() {
    return transaction.inTransactionMany(
        TransactionOptions.defaults().withReadOnly(),
        orderRepository::findAll);
  }
}
