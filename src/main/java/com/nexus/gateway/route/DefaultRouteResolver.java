package com.nexus.gateway.route;

import com.nexus.gateway.domain.RouteDefinition;
import com.nexus.gateway.exception.RouteNotFoundException;
import com.nexus.gateway.registry.GatewayRegistry;
import org.springframework.stereotype.Component;

@Component
public class DefaultRouteResolver implements RouteResolver {

    private final GatewayRegistry gatewayRegistry;
    private final RouteMatcher routeMatcher;

    public DefaultRouteResolver(
            GatewayRegistry gatewayRegistry,
            RouteMatcher routeMatcher
    ) {
        this.gatewayRegistry = gatewayRegistry;
        this.routeMatcher = routeMatcher;
    }

    @Override
    public RouteDefinition resolve(String requestPath) {
        return gatewayRegistry.getAllRoutes()
                .stream()
                .filter(route ->
                        routeMatcher.matches(
                                route.path(),
                                requestPath
                        )
                ).min((a, b) -> Integer.compare(
                        routeMatcher.specificity(b.path()),
                        routeMatcher.specificity(a.path())
                ))
                .orElseThrow(() -> new RouteNotFoundException(requestPath));
    }
}