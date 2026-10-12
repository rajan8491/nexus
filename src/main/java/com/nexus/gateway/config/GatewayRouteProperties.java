package com.nexus.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record GatewayRouteProperties(

        @NotBlank(message = "gateway.routes[].id must not be blank")
        String id,

        @NotBlank(message = "gateway.routes[].path must not be blank")
        @Pattern(
                regexp = "^/.*",
                message = "gateway.routes[].path must start with a forward slash '/'"
        )
        String path,

        @NotBlank(message = "gateway.routes[].service must not be blank")
        String service,

        @Valid
        RouteSecurityProperties security

) {
    public GatewayRouteProperties {
        if (security == null) {
            security = new RouteSecurityProperties(false, List.of());
        }
    }

    public record RouteSecurityProperties(
            boolean auth,
            List<String> roles
    ) {
        public RouteSecurityProperties {
            if (roles == null) {
                roles = List.of();
            }
        }
    }
}