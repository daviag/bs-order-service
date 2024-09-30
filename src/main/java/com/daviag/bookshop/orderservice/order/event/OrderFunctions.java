package com.daviag.bookshop.orderservice.order.event;

import com.daviag.bookshop.orderservice.order.domain.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class OrderFunctions {

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return flux -> orderService
                .consumeOrderDispatchedEvent(flux)
                .doOnNext(order -> log.info("The order with ID {} is dispatched.", order.id()))
                .subscribe();
    }
}
