package com.daviag.bookshop.orderservice.order.web;

import com.daviag.bookshop.orderservice.order.domain.Order;
import com.daviag.bookshop.orderservice.order.domain.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
@AllArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Flux<Order> getAllOrders(@AuthenticationPrincipal Jwt jwt) {
        log.info("Fetching all orders");
        return orderService.getAllOrders(jwt.getSubject());
    }

    @PostMapping
    public Mono<Order> submitOrder(@RequestBody @Valid OrderRequest orderRequest) {
        log.info("Order for {} copies of the book with ISBN {}", orderRequest.quantity(), orderRequest.isbn());
        return orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity());
    }
}
