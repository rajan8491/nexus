package com.nexus.gateway.route;

import jakarta.validation.constraints.NotBlank;

public record RouteDefinition(

        @NotBlank
        String id,

        @NotBlank
        String path,

        @NotBlank
        String service

) {
}