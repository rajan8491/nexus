package com.nexus.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(

        @NotEmpty(
                message = "Gateway must contain at least one route"
        )
        List<@Valid GatewayRouteProperties> routes,

        @NotEmpty(
                message = "Gateway must contain at least one service"
        )
        Map<String, @Valid GatewayServiceProperties> services

) {
}