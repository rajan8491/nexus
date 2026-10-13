# Nexus Request Forwarding Upgrade Checklist

This checklist tracks the phased upgrade of the request-forwarding layer. Work will proceed one phase at a time, and
each phase should be validated before moving to the next.

## Working rule

- [ ] Complete only the current phase
- [ ] Run focused tests for the current phase
- [ ] Fix regressions before starting the next phase
- [ ] Preserve routing, rate limiting, load balancing, and authentication behavior

## Phase 1 — Inspect before modifying

- [x] Inspect repository structure and current forwarding flow
- [x] Review `pom.xml`, Spring Boot / WebFlux version, and test layout
- [x] Trace headers, bodies, query parameters, target URI building, and upstream response flow
- [x] Identify reusable components and architectural risks
- [x] Record a concise implementation plan and affected files

### Phase 1 deliverable

Current forwarding path:

- `GatewayWebFilter`
- `GatewayHandler`
- `DefaultRouteResolver` / `DefaultRouteMatcher`
- `DefaultServiceResolver`
- `SmartLoadBalancer`
- `DefaultTargetUriBuilder`
- `WebClientRequestForwarder`

Key reuse points:

- existing reactive WebFlux stack
- existing `WebClient` bean setup
- existing routing/service registry model
- existing exception mapping

Main risks identified:

- forwarding semantics are not yet explicit for hop-by-hop headers and multi-value response headers
- request-body handling may need tighter reactive preservation
- transport/client policy is currently minimal and will likely need phased hardening

Phase 2 candidate files:

- `src/main/java/com/nexus/gateway/proxy/WebClientRequestForwarder.java`
- `src/main/java/com/nexus/gateway/proxy/RequestForwarder.java`
- `src/main/java/com/nexus/gateway/proxy/DefaultTargetUriBuilder.java`
- `src/main/java/com/nexus/gateway/handler/GatewayHandler.java`
- `src/main/java/com/nexus/gateway/exception/GlobalGatewayExceptionHandler.java`
- `src/test/java/com/nexus/gateway/...` forwarding tests to be added incrementally

## Phase 2 — Correct HTTP proxy semantics

- [x] Preserve HTTP method, path, query string, headers, and response semantics
- [x] Forward request bodies reactively without unnecessary buffering
- [ ] Preserve streaming behavior for large bodies
- [x] Preserve upstream status codes, bodies, and multi-value headers
- [x] Handle empty bodies and supported methods correctly
- [ ] Respect cancellation and backpressure
- [x] Handle URI encoding and route prefix rewriting safely

### Phase 2 implementation note

The forwarding layer now preserves the incoming method, path, query string, upstream status, headers, and body behavior
for the current gateway flow. The request body is forwarded reactively, and upstream 4xx/5xx responses are preserved
instead of being converted into gateway errors.

The remaining open items for a later hardening pass are true end-to-end streaming preservation for very large payloads
and explicit cancellation/backpressure verification.

## Phase 3 — Header security and forwarding policy

- [x] Add centralized request-header filtering
- [x] Remove hop-by-hop headers and `Connection`-named headers
- [x] Handle `Host`, `Content-Length`, `Transfer-Encoding`, `TE`, `Trailer`, and `Upgrade`
- [x] Prevent spoofed `Forwarded` / `X-Forwarded-*` propagation
- [x] Generate or propagate a correlation ID
- [x] Separate request-header and response-header filtering

## Phase 4 — HTTP client and connection management

- [X] Reuse shared managed client resources
- [X] Add connection pool and transport settings
- [X] Configure acquisition, connect, and response/read timeouts
- [X] Add graceful resource disposal
- [X] Avoid creating a new client per request

## Phase 5 — Timeouts, failure handling, and error responses

- [ ] Distinguish transport failures from valid upstream 4xx/5xx responses
- [ ] Map upstream transport failures to safe gateway errors
- [ ] Preserve committed upstream responses
- [ ] Emit consistent JSON errors with request IDs and safe messages
- [ ] Apply correct HTTP statuses for gateway-generated errors

## Phase 6 — Safe retries

- [ ] Add bounded retry semantics with backoff and jitter
- [ ] Prevent retries for non-replayable or unsafe requests by default
- [ ] Avoid retrying normal 4xx responses
- [ ] Coordinate retries with load balancing and failure policy
- [ ] Ensure retries respect the overall deadline

## Phase 7 — Circuit breaker and bulkhead

- [ ] Add route/service policy resolution with defaults
- [ ] Implement reactive circuit breaker behavior
- [ ] Implement non-blocking bulkhead limits
- [ ] Map open-breaker and bulkhead rejections safely
- [ ] Keep policies configurable through typed properties

## Phase 8 — Configuration and maintainability

- [ ] Add typed validated properties for transport, retry, breaker, and bulkhead settings
- [ ] Define clear precedence: route > service > gateway default
- [ ] Keep forwarding, policy, and observability concerns separated
- [ ] Avoid unnecessary abstraction layers

## Phase 9 — Observability

- [ ] Add bounded metrics for forwarding, retries, timeouts, breaker, and bulkhead events
- [ ] Add structured logs with correlation IDs and upstream identifiers
- [ ] Avoid sensitive or unbounded metric labels

## Phase 10 — Testing

- [ ] Add deterministic mock-upstream tests for forwarding behavior
- [ ] Cover headers, bodies, streaming, timeouts, retries, breakers, bulkheads, cancellation, and deadline behavior
- [ ] Run the full test suite and record actual results
- [ ] Document known limitations and remaining hardening work

## Current next step

- [ ] Implement Phase 2 only, after Phase 1 planning is approved

## Reference documents

- `PHASE_1_INSPECTION.md` — detailed current-state inspection and Phase 2 entry point

