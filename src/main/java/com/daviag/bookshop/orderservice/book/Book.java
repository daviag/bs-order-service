package com.daviag.bookshop.orderservice.book;

// see spring-cloud-contract
public record Book(
        String isbn,
        String title,
        String author,
        Double price
) {
}
