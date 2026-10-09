package com.nexus.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GatewayRouteProperties(

        @NotBlank(message = "Route id must not be blank")
        String id,

        @NotBlank(message = "Route path must not be blank")
        @Pattern(
                regexp = "/.*",
                message = "Route path must start with '/'"
        )
        String path,

        @NotBlank(message = "Route service must not be blank")
        String service

) {
}