package io.github.camilyed.transaction.demo.order.domain;

import java.math.BigDecimal;

public record PaymentReservation(String id, OrderId orderId, BigDecimal amount) {}
