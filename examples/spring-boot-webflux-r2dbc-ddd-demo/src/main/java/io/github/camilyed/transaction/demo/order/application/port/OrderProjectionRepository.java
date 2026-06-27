package io.github.camilyed.transaction.demo.order.application.port;

import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderProjection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderProjectionRepository {

  Mono<OrderProjection> rebuildFrom(Order order);

  Flux<OrderProjection> findAll();

  Mono<Void> deleteAll();
}
