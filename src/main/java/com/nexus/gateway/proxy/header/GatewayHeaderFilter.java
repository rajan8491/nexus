package com.nexus.gateway.proxy.header;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public interface GatewayHeaderFilter {
    HttpHeaders filterRequestHeaders(ServerWebExchange exchange);

    HttpHeaders filterResponseHeaders(HttpHeaders originalResponseHeaders);
}