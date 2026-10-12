package com.nexus.gateway.domain;

import java.util.List;

public record RouteDefinition(
        String id,
        String path,
        String service,
        boolean authRequired,
        List<String> roles
) {
    public RouteDefinition {
        if (roles == null) {
            roles = List.of();
        }
    }
}