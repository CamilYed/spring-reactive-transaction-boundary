package io.github.camilyed.transaction.demo.order.domain;

import java.math.BigDecimal;

public record OrderProjection(String orderId, String customerId, BigDecimal amount, String status) {}
