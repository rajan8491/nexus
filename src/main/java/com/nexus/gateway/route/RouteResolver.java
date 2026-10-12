package com.nexus.gateway.route;

import com.nexus.gateway.domain.RouteDefinition;

public interface RouteResolver {
    RouteDefinition resolve(String incomingPath);
}