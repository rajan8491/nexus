package com.nexus.gateway.route;

public interface RouteResolver {
    RouteDefinition resolve(String path);
}