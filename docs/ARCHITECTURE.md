# Nexus Gateway Architecture

This document captures the current architecture of the Nexus gateway so future tasks can reuse the same mental model.

## 1. Purpose
Nexus is a reactive API gateway that sits in front of downstream services and provides:

- request routing
- rate limiting
- load balancing
- downstream request forwarding
- centralized error handling
- active health checking

## 2. High-level request flow
1. `GatewayApplication` boots the Spring Boot/WebFlux app and binds configuration properties.
2. `RateLimitingWebFilter` evaluates the incoming request against configured rate-limit rules.
3. `GatewayWebFilter` delegates the request to `GatewayHandler`.
4. `GatewayHandler` performs the main lookup and forwarding flow:
   - resolve the matching route
   - resolve the backing service
   - choose a healthy instance through the load balancer
   - build the downstream target URI
   - forward the request using the WebClient-based forwarder
5. `GlobalGatewayExceptionHandler` converts gateway failures into consistent JSON responses.

## 3. Core packages

### `com.nexus.gateway.config`
Typed configuration model for gateway behavior, including:
- routes
- services
- service instances
- health checks
- rate limiting
- security

### `com.nexus.gateway.domain`
Runtime models created from configuration:
- `RouteDefinition`
- `ServiceDefinition`
- `ServiceInstance`
- `HealthCheckConfig`
- `RateLimitRuleDefinition`

### `com.nexus.gateway.registry`
`GatewayRegistry` builds the runtime registry from configuration and acts as the central source of truth for routes, services, health checks, and rate-limit rules.

### `com.nexus.gateway.route`
Route lookup and matching:
- `RouteMatcher` / `DefaultRouteMatcher`
- `RouteResolver` / `DefaultRouteResolver`

### `com.nexus.gateway.service`
Service lookup and validation:
- `ServiceResolver` / `DefaultServiceResolver`

### `com.nexus.gateway.loadbalancer`
Instance selection logic:
- `LoadBalancer`
- `SmartLoadBalancer`
- `loadbalancer.strategy.*`

### `com.nexus.gateway.proxy`
Downstream request construction and forwarding:
- `TargetUriBuilder` / `DefaultTargetUriBuilder`
- `RequestForwarder` / `WebClientRequestForwarder`

### `com.nexus.gateway.health`
`ActiveHealthChecker` runs active health checks against configured service instances.

### `com.nexus.gateway.ratelimit`
Rate-limiting enforcement and strategy registry:
- `RateLimitingWebFilter`
- `RateLimiterRegistry`
- `ratelimit.strategy.*`

### `com.nexus.gateway.exception`
Gateway-specific exceptions plus the global exception handler.

## 4. Main extension points
The architecture is intentionally modular. The most likely extension points are:

- **route matching**: extend or replace route resolver/matcher logic
- **service resolution**: change how services are discovered or selected
- **load balancing**: add new `LoadBalancerStrategy` implementations
- **target URI construction**: customize how downstream URIs are built
- **request forwarding**: replace the WebClient forwarder if needed
- **rate limiting**: add new rate-limit strategies or algorithms
- **health checking**: modify active health-check behavior or schedule

## 5. Testing focus
The current tests mainly cover:

- application context bootstrap
- route matching behavior
- rate-limit registry and filter behavior
- load-balancing behavior
- strategy-level unit tests

## 6. Notes for future tasks
When changing gateway behavior, check these layers in order:

1. configuration model in `config/`
2. runtime model in `domain/`
3. registry assembly in `registry/`
4. routing/service/load-balancing flow
5. proxying and error handling
6. tests for the affected layer

This ordering keeps changes consistent and easier to reason about.

