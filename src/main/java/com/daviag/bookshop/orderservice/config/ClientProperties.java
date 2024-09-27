package com.daviag.bookshop.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "bs")
public record ClientProperties (
        @NotNull
        URI catalogServiceUri
) {
}
