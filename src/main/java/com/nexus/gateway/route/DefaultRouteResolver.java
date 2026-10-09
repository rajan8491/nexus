package com.nexus.gateway.route;

import com.nexus.gateway.config.GatewayProperties;
import com.nexus.gateway.config.GatewayRouteProperties;
import com.nexus.gateway.exception.RouteNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class DefaultRouteResolver implements RouteResolver {

    private final GatewayProperties gatewayProperties;
    private final RouteMatcher routeMatcher;

    public DefaultRouteResolver(
            GatewayProperties gatewayProperties,
            RouteMatcher routeMatcher
    ) {
        this.gatewayProperties = gatewayProperties;
        this.routeMatcher = routeMatcher;
    }

    @Override
    public RouteDefinition resolve(String requestPath) {

        return gatewayProperties.routes()
                .stream()
                .filter(route ->
                        routeMatcher.matches(
                                route.path(),
                                requestPath
                        )
                )
                .sorted(
                        (a, b) -> Integer.compare(
                                routeMatcher.specificity(b.path()),
                                routeMatcher.specificity(a.path())
                        )
                )
                .map(this::toRouteDefinition)
                .findFirst()
                .orElseThrow(() ->
                        new RouteNotFoundException(requestPath)
                );
    }

    private RouteDefinition toRouteDefinition(
            GatewayRouteProperties route
    ) {

        return new RouteDefinition(
                route.id(),
                route.path(),
                route.service()
        );
    }
}
