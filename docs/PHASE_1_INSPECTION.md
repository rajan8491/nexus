# Phase 1 Inspection Report — Nexus Request Forwarding Upgrade

## Checklist
- [x] Inspect repository structure and forwarding flow
- [x] Review build and runtime configuration
- [x] Trace request/response data through the gateway
- [x] Identify reusable components and risks
- [x] Capture Phase 2 implementation candidates

## 1. Repository and runtime snapshot
- **Build tool:** Maven (`pom.xml`)
- **Framework:** Spring Boot + Spring WebFlux
- **Application model:** reactive gateway, not Spring MVC
- **Current overall shape:** clean modular split across routing, service lookup, load balancing, proxying, rate limiting, health checks, and exception handling

## 2. Current request-forwarding path
The current path is:

`GatewayWebFilter -> GatewayHandler -> RouteResolver -> ServiceResolver -> SmartLoadBalancer -> TargetUriBuilder -> WebClientRequestForwarder -> response write`

### What each stage does today
- `GatewayWebFilter` is the entry point that invokes gateway processing and writes the proxied response back to the exchange.
- `GatewayHandler` orchestrates request handling and composes routing, service lookup, load balancing, URI building, and forwarding.
- `DefaultRouteResolver` and `DefaultRouteMatcher` select the matching route from configured routes.
- `DefaultServiceResolver` resolves the target service definition.
- `SmartLoadBalancer` filters unhealthy instances and delegates selection to a strategy.
- `DefaultTargetUriBuilder` constructs the downstream URI from the selected instance plus incoming path/query.
- `WebClientRequestForwarder` performs the actual downstream WebClient call.
- `GlobalGatewayExceptionHandler` maps gateway exceptions to HTTP responses.

## 3. Reusable components already in place
### Forwarding stack
- `RequestForwarder` interface already exists as the forwarding abstraction.
- `WebClientRequestForwarder` already provides the transport path to reuse rather than replace.
- `DefaultTargetUriBuilder` already centralizes target URI construction.

### Routing and selection
- `GatewayRegistry` already materializes routes, services, and config into runtime domain objects.
- `SmartLoadBalancer` already handles unhealthy-instance filtering before strategy-based selection.
- `RoundRobinStrategy` and `RandomStrategy` already provide load-balancing variants.

### Error handling
- `GatewayException`, `RouteNotFoundException`, `ServiceNotFoundException`, and `NoAvailableInstanceException` already define the gateway's error surface.
- `GlobalGatewayExceptionHandler` already centralizes JSON error mapping.

### Configuration
- `GatewayProperties` and its nested config records provide typed gateway configuration.
- Existing route/service config already gives enough information to drive forwarding policies later.

## 4. Request/response flow observations
### Request path
- The current implementation resolves the route and service before forwarding.
- Query handling appears to be centralized in `DefaultTargetUriBuilder`.
- Load balancing already occurs before URI generation and forwarding.

### Headers
- Headers are not yet governed by an explicit centralized forwarding policy.
- There is no dedicated request-header filtering layer visible in the current proxy path.
- Response-header filtering is also not isolated as a separate concern.

### Body handling
- The forwarding path is reactive and WebClient-based, but the body-preservation semantics are not explicitly documented in code.
- There is no visible dedicated policy for replayability, buffering limits, or retry-aware body handling.

### Upstream responses
- The gateway currently forwards through the proxy stack and returns downstream results, but there is no dedicated response-semantic layer for preserving all upstream headers/statuses in a controlled way.

## 5. Risks and gaps identified
- **Hop-by-hop headers:** no explicit centralized removal policy is visible yet.
- **Forwarded spoofing:** client-supplied forwarding headers are not yet governed by a clear trust policy.
- **Request body replayability:** no policy exists for safe retries or non-replayable bodies.
- **Timeouts and transport policy:** current WebClient setup is minimal and does not yet expose transport tuning.
- **Failure mapping:** not all upstream failure categories are separated yet.
- **Observability:** there is no dedicated forwarding metrics layer yet.
- **Testing:** forwarding behavior lacks dedicated end-to-end proxy tests with local mock upstreams.

## 6. Files most likely to change in Phase 2
- `src/main/java/com/nexus/gateway/proxy/WebClientRequestForwarder.java`
- `src/main/java/com/nexus/gateway/proxy/RequestForwarder.java`
- `src/main/java/com/nexus/gateway/proxy/DefaultTargetUriBuilder.java`
- `src/main/java/com/nexus/gateway/handler/GatewayHandler.java`
- `src/main/java/com/nexus/gateway/exception/GlobalGatewayExceptionHandler.java`
- `src/test/java/com/nexus/gateway/...` forwarding tests to be added incrementally

## 7. Recommended Phase 2 entry point
Start with the proxy layer itself:
1. inspect `WebClientRequestForwarder`
2. preserve request method/path/query/body semantics
3. add header-policy helpers only if needed for that step
4. add deterministic forwarding tests before moving into transport tuning or resilience

## 8. Final Phase 1 conclusion
Phase 1 is complete as an inspection and planning step. The codebase is ready for a focused Phase 2 implementation without rewriting the overall architecture.

