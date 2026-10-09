package com.nexus.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GatewayServiceProperties(

        @NotEmpty(
                message = "Service must contain at least one instance"
        )
        List<@Valid GatewayServiceInstanceProperties> instances

) {
}
