package com.nexus.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URI;

public record GatewayServiceInstanceProperties(

        @NotBlank(message = "Service instance id must not be blank")
        String id,

        @NotNull(message = "Service instance URI must not be null")
        URI uri

) {
}