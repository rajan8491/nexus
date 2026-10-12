package com.nexus.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GatewayServiceProperties(

        String loadBalancer, // Optional: if omitted, registry falls back to global default

        @NotEmpty(message = "Each service must define at least one active instance")
        List<@Valid GatewayServiceInstanceProperties> instances

) {
}