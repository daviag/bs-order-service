package com.daviag.bookshop.orderservice.order.web;

import com.daviag.bookshop.orderservice.config.SecurityConfig;
import com.daviag.bookshop.orderservice.order.domain.Order;
import com.daviag.bookshop.orderservice.order.domain.OrderService;
import com.daviag.bookshop.orderservice.order.domain.OrderStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(OrderController.class)
@Import(SecurityConfig.class)
public class OrderControllerWebFluxTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void whenRequestAuthorizedBookNotAvailableThenRejectOrder() {
        var orderRequest = new OrderRequest("1234567890", 3);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn(), orderRequest.quantity());

        BDDMockito.given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers
                        .mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_customer")))
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(o -> {
                   assertThat(o).isNotNull();
                   assertThat(o.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }

    @Test
    void whenRequestUnauthenticatedThen401() {
        var orderRequest = new OrderRequest("1234567890", 3);

        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                ;
    }


}
