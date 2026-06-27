package io.github.camilyed.transaction.demo.order.adapter.out.r2dbc;

import io.github.camilyed.transaction.demo.order.application.port.PaymentReservationRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.PaymentReservation;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcPaymentReservationRepository implements PaymentReservationRepository {

  private final DatabaseClient databaseClient;

  public R2dbcPaymentReservationRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  @Override
  public Mono<PaymentReservation> reserveFor(Order order) {
    var reservation = new PaymentReservation(UUID.randomUUID().toString(), order.id(), order.amount());

    return databaseClient
        .sql(
            """
            INSERT INTO payment_reservations(id, order_id, amount)
            VALUES (:id, :orderId, :amount)
            """)
        .bind("id", reservation.id())
        .bind("orderId", reservation.orderId().value())
        .bind("amount", reservation.amount())
        .fetch()
        .rowsUpdated()
        .thenReturn(reservation);
  }

  @Override
  public Mono<Long> count() {
    return databaseClient
        .sql("SELECT COUNT(*) AS reservation_count FROM payment_reservations")
        .map((row, metadata) -> row.get("reservation_count", Long.class))
        .one();
  }
}
