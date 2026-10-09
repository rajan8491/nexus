package com.nexus.gateway.proxy;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class DefaultTargetUriBuilder
        implements TargetUriBuilder {

    @Override
    public URI build(
            URI serviceUri,
            ServerWebExchange exchange
    ) {

        return UriComponentsBuilder
                .fromUri(serviceUri)
                .path(exchange.getRequest().getURI().getPath())
                .query(exchange.getRequest().getURI().getRawQuery())
                .build(true)
                .toUri();
    }
}