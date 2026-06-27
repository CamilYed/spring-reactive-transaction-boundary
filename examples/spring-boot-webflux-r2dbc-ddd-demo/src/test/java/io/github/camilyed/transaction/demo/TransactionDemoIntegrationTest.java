package io.github.camilyed.transaction.demo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.camilyed.transaction.ReactiveTransaction;
import io.github.camilyed.transaction.demo.order.application.ConfirmOrderUseCase;
import io.github.camilyed.transaction.demo.order.application.CreateOrderUseCase;
import io.github.camilyed.transaction.demo.order.application.CreateOrderWithFailureUseCase;
import io.github.camilyed.transaction.demo.order.application.ListOrdersUseCase;
import io.github.camilyed.transaction.demo.order.application.RebuildOrderProjectionUseCase;
import io.github.camilyed.transaction.demo.order.application.port.OrderRepository;
import io.github.camilyed.transaction.demo.order.application.port.PaymentReservationRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

@SpringBootTest
class TransactionDemoIntegrationTest extends PostgreSqlTestContainerSupport {

  @Autowired
  private ReactiveTransaction transaction;

  @Autowired
  private CreateOrderUseCase createOrderUseCase;

  @Autowired
  private CreateOrderWithFailureUseCase createOrderWithFailureUseCase;

  @Autowired
  private ListOrdersUseCase listOrdersUseCase;

  @Autowired
  private ConfirmOrderUseCase confirmOrderUseCase;

  @Autowired
  private RebuildOrderProjectionUseCase rebuildOrderProjectionUseCase;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private PaymentReservationRepository paymentReservationRepository;

  @Autowired
  private DatabaseClient databaseClient;

  @BeforeEach
  void cleanDatabase() {
    StepVerifier.create(databaseClient.sql("DELETE FROM order_projections").then()).verifyComplete();
    StepVerifier.create(databaseClient.sql("DELETE FROM payment_reservations").then()).verifyComplete();
    StepVerifier.create(databaseClient.sql("DELETE FROM demo_orders").then()).verifyComplete();
  }

  @Test
  void shouldAutoConfigureReactiveTransactionFromSnapshotStarter() {
    // expect
    assertThat(transaction).isNotNull();
  }

  @Test
  void shouldCommitOrderAndPaymentReservationInSingleTransaction() {
    // Given
    var customerId = "customer-1";
    var amount = new BigDecimal("49.99");

    // When
    var result = createOrderUseCase.handle(customerId, amount);

    // Then
    StepVerifier.create(result)
        .assertNext(orderId -> assertThat(orderId.value()).isNotBlank())
        .verifyComplete();

    StepVerifier.create(orderRepository.count()).expectNext(1L).verifyComplete();
    StepVerifier.create(paymentReservationRepository.count()).expectNext(1L).verifyComplete();
  }

  @Test
  void shouldRollbackOrderAndPaymentReservationWhenUseCaseFails() {
    // Given
    var customerId = "customer-rollback";
    var amount = new BigDecimal("19.99");

    // When
    var result = createOrderWithFailureUseCase.handle(customerId, amount);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(error -> error.getMessage().contains("Simulated failure"))
        .verify();

    StepVerifier.create(orderRepository.count()).expectNext(0L).verifyComplete();
    StepVerifier.create(paymentReservationRepository.count()).expectNext(0L).verifyComplete();
  }

  @Test
  void shouldListOrdersUsingReadOnlyFluxTransaction() {
    // Given
    StepVerifier.create(createOrderUseCase.handle("customer-a", new BigDecimal("10.00")))
        .expectNextCount(1)
        .verifyComplete();
    StepVerifier.create(createOrderUseCase.handle("customer-b", new BigDecimal("20.00")))
        .expectNextCount(1)
        .verifyComplete();

    // When
    var result = listOrdersUseCase.handle();

    // Then
    StepVerifier.create(result).expectNextCount(2).verifyComplete();
  }

  @Test
  void shouldConfirmOrderUsingSerializableRequiresNewTransaction() {
    // Given
    var orderId = createOrderUseCase.handle("customer-confirm", new BigDecimal("99.00")).block();

    // When
    var result = confirmOrderUseCase.handle(orderId);

    // Then
    StepVerifier.create(result)
        .assertNext(order -> assertThat(order.status().name()).isEqualTo("CONFIRMED"))
        .verifyComplete();
  }

  @Test
  void shouldRebuildOrderProjectionUsingFluxTransaction() {
    // Given
    StepVerifier.create(createOrderUseCase.handle("customer-projection-1", new BigDecimal("15.00")))
        .expectNextCount(1)
        .verifyComplete();
    StepVerifier.create(createOrderUseCase.handle("customer-projection-2", new BigDecimal("25.00")))
        .expectNextCount(1)
        .verifyComplete();

    // When
    var result = rebuildOrderProjectionUseCase.handle();

    // Then
    StepVerifier.create(result).expectNextCount(2).verifyComplete();
  }
}
