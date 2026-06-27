package io.github.camilyed.transaction.demo.order.domain;

import java.util.UUID;

public record OrderId(String value) {

  public static OrderId newId() {
    return new OrderId(UUID.randomUUID().toString());
  }
}
