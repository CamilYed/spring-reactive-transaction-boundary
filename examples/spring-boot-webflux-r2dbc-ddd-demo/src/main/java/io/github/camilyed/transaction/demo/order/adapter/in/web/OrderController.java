package io.github.camilyed.transaction.demo.order.adapter.in.web;

import io.github.camilyed.transaction.demo.order.application.ConfirmOrderUseCase;
import io.github.camilyed.transaction.demo.order.application.CreateOrderUseCase;
import io.github.camilyed.transaction.demo.order.application.CreateOrderWithFailureUseCase;
import io.github.camilyed.transaction.demo.order.application.ListOrdersUseCase;
import io.github.camilyed.transaction.demo.order.application.RebuildOrderProjectionUseCase;
import io.github.camilyed.transaction.demo.order.application.port.OrderProjectionRepository;
import io.github.camilyed.transaction.demo.order.domain.Order;
import io.github.camilyed.transaction.demo.order.domain.OrderId;
import io.github.camilyed.transaction.demo.order.domain.OrderProjection;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
final class OrderController {

  private final CreateOrderUseCase createOrderUseCase;
  private final CreateOrderWithFailureUseCase createOrderWithFailureUseCase;
  private final ListOrdersUseCase listOrdersUseCase;
  private final ConfirmOrderUseCase confirmOrderUseCase;
  private final RebuildOrderProjectionUseCase rebuildOrderProjectionUseCase;
  private final OrderProjectionRepository projectionRepository;

  OrderController(
      CreateOrderUseCase createOrderUseCase,
      CreateOrderWithFailureUseCase createOrderWithFailureUseCase,
      ListOrdersUseCase listOrdersUseCase,
      ConfirmOrderUseCase confirmOrderUseCase,
      RebuildOrderProjectionUseCase rebuildOrderProjectionUseCase,
      OrderProjectionRepository projectionRepository) {
    this.createOrderUseCase = createOrderUseCase;
    this.createOrderWithFailureUseCase = createOrderWithFailureUseCase;
    this.listOrdersUseCase = listOrdersUseCase;
    this.confirmOrderUseCase = confirmOrderUseCase;
    this.rebuildOrderProjectionUseCase = rebuildOrderProjectionUseCase;
    this.projectionRepository = projectionRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  Mono<CreateOrderResponse> create(@RequestBody CreateOrderRequest request) {
    return createOrderUseCase
        .handle(request.customerId(), request.amount())
        .map(orderId -> new CreateOrderResponse(orderId.value()));
  }

  @PostMapping("/failing")
  Mono<CreateOrderResponse> createWithFailure(@RequestBody CreateOrderRequest request) {
    return createOrderWithFailureUseCase
        .handle(request.customerId(), request.amount())
        .map(orderId -> new CreateOrderResponse(orderId.value()));
  }

  @GetMapping
  Flux<OrderResponse> list() {
    return listOrdersUseCase.handle().map(OrderResponse::from);
  }

  @PostMapping("/{orderId}/confirm")
  Mono<OrderResponse> confirm(@PathVariable String orderId) {
    return confirmOrderUseCase.handle(new OrderId(orderId)).map(OrderResponse::from);
  }

  @PostMapping("/projections/rebuild")
  Flux<OrderProjection> rebuildProjections() {
    return rebuildOrderProjectionUseCase.handle();
  }

  @GetMapping("/projections")
  Flux<OrderProjection> projections() {
    return projectionRepository.findAll();
  }

  record CreateOrderRequest(String customerId, BigDecimal amount) {}

  record CreateOrderResponse(String orderId) {}

  record OrderResponse(String id, String customerId, BigDecimal amount, String status) {

    static OrderResponse from(Order order) {
      return new OrderResponse(
          order.id().value(), order.customerId(), order.amount(), order.status().name());
    }
  }
}
