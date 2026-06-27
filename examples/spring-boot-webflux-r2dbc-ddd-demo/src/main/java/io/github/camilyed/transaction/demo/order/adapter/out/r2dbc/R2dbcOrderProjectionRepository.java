package io.github.camilyed.transaction.demo.order.adapter.out.r2dbc;

import io.github.camilyed.transaction.demo.order.application.port.OrderProjectionRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderProjection;
import java.math.BigDecimal;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcOrderProjectionRepository implements OrderProjectionRepository {

  private final DatabaseClient databaseClient;

  public R2dbcOrderProjectionRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  @Override
  public Mono<OrderProjection> rebuildFrom(Order order) {
    var projection =
        new OrderProjection(
            order.id().value(), order.customerId(), order.amount(), order.status().name());

    return databaseClient
        .sql(
            """
            INSERT INTO order_projections(order_id, customer_id, amount, status)
            VALUES (:orderId, :customerId, :amount, :status)
            """)
        .bind("orderId", projection.orderId())
        .bind("customerId", projection.customerId())
        .bind("amount", projection.amount())
        .bind("status", projection.status())
        .fetch()
        .rowsUpdated()
        .thenReturn(projection);
  }

  @Override
  public Flux<OrderProjection> findAll() {
    return databaseClient
        .sql(
            """
            SELECT order_id, customer_id, amount, status
            FROM order_projections
            ORDER BY order_id
            """)
        .map(
            (row, metadata) ->
                new OrderProjection(
                    row.get("order_id", String.class),
                    row.get("customer_id", String.class),
                    row.get("amount", BigDecimal.class),
                    row.get("status", String.class)))
        .all();
  }

  @Override
  public Mono<Void> deleteAll() {
    return databaseClient.sql("DELETE FROM order_projections").then();
  }
}
