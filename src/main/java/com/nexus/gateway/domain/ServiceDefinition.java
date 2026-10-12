package com.nexus.gateway.domain;

import java.util.List;

public record ServiceDefinition(
        String name,
        String loadBalancer,
        List<ServiceInstance> instances
) {
}