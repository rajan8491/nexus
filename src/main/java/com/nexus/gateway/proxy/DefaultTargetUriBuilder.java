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

        String rawPath = exchange.getRequest().getURI().getRawPath();
        String rawQuery = exchange.getRequest().getURI().getRawQuery();

        return UriComponentsBuilder
                .fromUri(serviceUri)
                .path(rawPath)
                .query(rawQuery)
                .build(true)
                .toUri();
    }
}