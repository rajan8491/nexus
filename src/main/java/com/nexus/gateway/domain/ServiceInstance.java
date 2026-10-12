package com.nexus.gateway.domain;

import java.net.URI;

public record ServiceInstance(
        String id,
        URI uri
) {
}