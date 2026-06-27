package io.github.camilyed.transaction.demo.order.application;

import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.demo.order.application.port.OrderProjectionRepository;
import io.github.camilyed.transaction.demo.order.application.port.OrderRepository;
import io.github.camilyed.transaction.demo.order.domain.OrderProjection;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public final class RebuildOrderProjectionUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;
  private final OrderProjectionRepository projectionRepository;

  public RebuildOrderProjectionUseCase(
      ReactiveTransaction transaction,
      OrderRepository orderRepository,
      OrderProjectionRepository projectionRepository) {
    this.transaction = transaction;
    this.orderRepository = orderRepository;
    this.projectionRepository = projectionRepository;
  }

  public Flux<OrderProjection> handle() {
    return transaction.inTransactionMany(
        () ->
            projectionRepository
                .deleteAll()
                .thenMany(orderRepository.findAll())
                .concatMap(projectionRepository::rebuildFrom));
  }
}
