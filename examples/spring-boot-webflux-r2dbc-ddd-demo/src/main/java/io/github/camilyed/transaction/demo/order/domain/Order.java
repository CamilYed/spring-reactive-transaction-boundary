package io.github.camilyed.transaction.demo.order.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Order(OrderId id, String customerId, BigDecimal amount, OrderStatus status) {

  public Order {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(customerId, "customerId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(status, "status must not be null");
  }

  public static Order newOrder(String customerId, BigDecimal amount) {
    return new Order(OrderId.newId(), customerId, amount, OrderStatus.NEW);
  }

  public Order confirm() {
    return new Order(id, customerId, amount, OrderStatus.CONFIRMED);
  }
}
