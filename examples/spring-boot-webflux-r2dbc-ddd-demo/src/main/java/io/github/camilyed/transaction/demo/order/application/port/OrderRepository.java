package io.github.camilyed.transaction.demo.order.application.port;

import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository {

  Mono<Order> save(Order order);

  Mono<Order> update(Order order);

  Mono<Order> findById(OrderId orderId);

  Flux<Order> findAll();

  Mono<Long> count();
}
