package io.github.camilyed.transaction.demo.order.application.port;

import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.PaymentReservation;
import reactor.core.publisher.Mono;

public interface PaymentReservationRepository {

  Mono<PaymentReservation> reserveFor(Order order);

  Mono<Long> count();
}
