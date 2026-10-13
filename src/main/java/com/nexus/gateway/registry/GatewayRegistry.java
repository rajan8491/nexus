package com.nexus.gateway.registry;

import com.nexus.gateway.config.GatewayHttpClientProperties;
import com.nexus.gateway.config.GatewayProperties;
import com.nexus.gateway.config.GatewaySecurityProperties;
import com.nexus.gateway.domain.*;
import com.nexus.gateway.ratelimit.Algorithm;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GatewayRegistry {

    private final GatewayProperties properties;
    private final Map<String, RouteDefinition> routesById = new ConcurrentHashMap<>();
    private final Map<String, ServiceDefinition> servicesByName = new ConcurrentHashMap<>();
    private HttpClientDefinition httpClientDefinition;
    @Getter
    private HealthCheckConfig healthCheckConfig;

    @Getter
    private boolean rateLimitingEnabled;
    @Getter
    private List<RateLimitRuleDefinition> rateLimitRules;
    @Getter
    private String defaultLoadBalancer;

    public GatewayRegistry(GatewayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        this.defaultLoadBalancer = (properties.defaultLoadBalancer() != null && !properties.defaultLoadBalancer().isBlank())
                ? properties.defaultLoadBalancer()
                : "ROUND_ROBIN";

        // Map Health Check Config
        if (properties.healthCheck() != null) {
            this.healthCheckConfig = new HealthCheckConfig(
                    properties.healthCheck().enabled(),
                    properties.healthCheck().intervalSeconds(),
                    properties.healthCheck().timeoutMillis(),
                    properties.healthCheck().path()
            );
        } else {
            this.healthCheckConfig = new HealthCheckConfig(true, 10, 2000, "/actuator/health");
        }

        // rate
        if (properties.rateLimiting() != null) {
            this.rateLimitingEnabled = properties.rateLimiting().enabled();
            this.rateLimitRules = properties.rateLimiting().rules().stream()
                    .map(rule -> new RateLimitRuleDefinition(
                            rule.id(),
                            rule.path(),
                            rule.method(),
                            RateLimitRuleDefinition.KeyType.valueOf(rule.keyType().name()),
                            rule.headerName(),
                            Algorithm.valueOf(rule.algorithm().toUpperCase()),
                            rule.capacity(),
                            rule.refillRate(),
                            rule.windowSeconds()
                    ))
                    .toList();
        } else {
            this.rateLimitingEnabled = false;
            this.rateLimitRules = List.of();
        }

        // Map Routes
        if (properties.routes() != null) {
            properties.routes().forEach(routeProp -> {
                RouteDefinition route = new RouteDefinition(
                        routeProp.id(),
                        routeProp.path(),
                        routeProp.service(),
                        false,
                        List.of()
                );
                routesById.put(route.id(), route);
            });
        }

        // Map Services
        if (properties.services() != null) {
            properties.services().forEach((serviceName, serviceProp) -> {
                List<ServiceInstance> instances = serviceProp.instances() == null
                        ? List.of()
                        : serviceProp.instances().stream()
                        .map(inst -> new ServiceInstance(inst.id(), inst.uri()))
                        .toList();

                String lbAlgorithm = (serviceProp.loadBalancer() != null && !serviceProp.loadBalancer().isBlank())
                        ? serviceProp.loadBalancer()
                        : this.defaultLoadBalancer;

                servicesByName.put(serviceName, new ServiceDefinition(serviceName, lbAlgorithm, instances));
            });
        }

        var httpProps = properties.httpClient() != null ? properties.httpClient()
                : new GatewayHttpClientProperties(null, null, null, null, null, null, null, null);

        this.httpClientDefinition = new HttpClientDefinition(
                httpProps.maxConnections(),
                httpProps.pendingAcquireMaxCount(),
                httpProps.pendingAcquireTimeout(),
                httpProps.maxIdleTime(),
                httpProps.maxLifeTime(),
                httpProps.connectTimeout(),
                httpProps.responseTimeout(),
                httpProps.readTimeout()
        );
    }

    public HttpClientDefinition getHttpClientConfig() {
        return httpClientDefinition;
    }

    public GatewaySecurityProperties getSecurityConfig() {
        return properties.security();
    }

    public List<RouteDefinition> getAllRoutes() {
        return List.copyOf(routesById.values());
    }

    public Optional<RouteDefinition> getRoute(String routeId) {
        return Optional.ofNullable(routesById.get(routeId));
    }

    public Optional<ServiceDefinition> getService(String serviceName) {
        return Optional.ofNullable(servicesByName.get(serviceName));
    }

    public List<ServiceDefinition> getAllServices() {
        return List.copyOf(servicesByName.values());
    }

}