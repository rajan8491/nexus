package com.nexus.gateway.proxy;

import org.springframework.web.server.ServerWebExchange;

import java.net.URI;

public interface TargetUriBuilder {

    URI build(
            URI serviceUri,
            ServerWebExchange exchange
    );
}
