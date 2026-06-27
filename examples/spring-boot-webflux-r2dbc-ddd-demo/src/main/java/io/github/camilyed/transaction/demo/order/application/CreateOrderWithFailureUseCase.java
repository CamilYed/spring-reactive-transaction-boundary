package io.github.camilyed.transaction.demo.order.application;

import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.demo.order.application.port.OrderRepository;
import io.github.camilyed.transaction.demo.order.application.port.PaymentReservationRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderId;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public final class CreateOrderWithFailureUseCase {

  private final ReactiveTransaction transaction;
  private final OrderRepository orderRepository;
  private final PaymentReservationRepository paymentReservationRepository;

  public CreateOrderWithFailureUseCase(
      ReactiveTransaction transaction,
      OrderRepository orderRepository,
      PaymentReservationRepository paymentReservationRepository) {
    this.transaction = transaction;
    this.orderRepository = orderRepository;
    this.paymentReservationRepository = paymentReservationRepository;
  }

  public Mono<OrderId> handle(String customerId, BigDecimal amount) {
    return transaction.inTransaction(
        () ->
            orderRepository
                .save(Order.newOrder(customerId, amount))
                .flatMap(paymentReservationRepository::reserveFor)
                .then(Mono.error(new IllegalStateException("Simulated failure after database writes"))));
  }
}
