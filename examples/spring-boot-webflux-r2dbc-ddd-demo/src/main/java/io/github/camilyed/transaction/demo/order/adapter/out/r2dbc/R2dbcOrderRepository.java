package io.github.camilyed.transaction.demo.order.adapter.out.r2dbc;

import io.github.camilyed.transaction.demo.order.application.port.OrderRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderId;
import io.github.camilyed.transaction.demo.order.domain.OrderStatus;
import java.math.BigDecimal;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcOrderRepository implements OrderRepository {

  private final DatabaseClient databaseClient;

  public R2dbcOrderRepository(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  @Override
  public Mono<Order> save(Order order) {
    return databaseClient
        .sql(
            """
            INSERT INTO demo_orders(id, customer_id, amount, status)
            VALUES (:id, :customerId, :amount, :status)
            """)
        .bind("id", order.id().value())
        .bind("customerId", order.customerId())
        .bind("amount", order.amount())
        .bind("status", order.status().name())
        .fetch()
        .rowsUpdated()
        .thenReturn(order);
  }

  @Override
  public Mono<Order> update(Order order) {
    return databaseClient
        .sql(
            """
            UPDATE demo_orders
            SET customer_id = :customerId,
                amount = :amount,
                status = :status
            WHERE id = :id
            """)
        .bind("id", order.id().value())
        .bind("customerId", order.customerId())
        .bind("amount", order.amount())
        .bind("status", order.status().name())
        .fetch()
        .rowsUpdated()
        .thenReturn(order);
  }

  @Override
  public Mono<Order> findById(OrderId orderId) {
    return databaseClient
        .sql(
            """
            SELECT id, customer_id, amount, status
            FROM demo_orders
            WHERE id = :id
            """)
        .bind("id", orderId.value())
        .map(
            (row, metadata) ->
                new Order(
                    new OrderId(row.get("id", String.class)),
                    row.get("customer_id", String.class),
                    row.get("amount", BigDecimal.class),
                    OrderStatus.valueOf(row.get("status", String.class))))
        .one();
  }

  @Override
  public Flux<Order> findAll() {
    return databaseClient
        .sql(
            """
            SELECT id, customer_id, amount, status
            FROM demo_orders
            ORDER BY id
            """)
        .map(
            (row, metadata) ->
                new Order(
                    new OrderId(row.get("id", String.class)),
                    row.get("customer_id", String.class),
                    row.get("amount", BigDecimal.class),
                    OrderStatus.valueOf(row.get("status", String.class))))
        .all();
  }

  @Override
  public Mono<Long> count() {
    return databaseClient
        .sql("SELECT COUNT(*) AS order_count FROM demo_orders")
        .map((row, metadata) -> row.get("order_count", Long.class))
        .one();
  }
}
