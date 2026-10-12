package com.nexus.gateway.config;

import java.util.List;

public record GatewaySecurityProperties(

        boolean enabled,

        String privateKeyLocation,

        List<String> publicPaths

) {
    public GatewaySecurityProperties {
        if (publicPaths == null) {
            publicPaths = List.of();
        }
    }
}