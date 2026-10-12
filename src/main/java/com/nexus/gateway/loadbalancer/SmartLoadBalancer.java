package com.nexus.gateway.loadbalancer;

import com.nexus.gateway.domain.ServiceDefinition;
import com.nexus.gateway.domain.ServiceInstance;
import com.nexus.gateway.exception.NoAvailableInstanceException;
import com.nexus.gateway.health.ActiveHealthChecker;
import com.nexus.gateway.loadbalancer.strategy.LoadBalancerStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Primary
@Slf4j
public class SmartLoadBalancer implements LoadBalancer {

    private final ActiveHealthChecker healthChecker;
    private final Map<String, LoadBalancerStrategy> strategies;

    public SmartLoadBalancer(
            ActiveHealthChecker healthChecker,
            List<LoadBalancerStrategy> strategyList
    ) {
        this.healthChecker = healthChecker;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        s -> s.getAlgorithm().toUpperCase(),
                        Function.identity()
                ));
    }

    @Override
    public ServiceInstance choose(ServiceDefinition service) {
        List<ServiceInstance> healthyInstances = service.instances().stream()
                .filter(healthChecker::isHealthy)
                .toList();

        if (healthyInstances.isEmpty()) {
            throw new NoAvailableInstanceException("No healthy instances available for service: " + service.name());
        }

        // The algorithm is resolved by GatewayRegistry onto ServiceDefinition at initialization
        String algorithm = service.loadBalancer() != null ? service.loadBalancer().toUpperCase() : "ROUND_ROBIN";

        LoadBalancerStrategy strategy = strategies.getOrDefault(algorithm, strategies.get("ROUND_ROBIN"));
        ServiceInstance selectedInstance = strategy.choose(healthyInstances);

        log.info(
                "Selected service instance [{}] at [{}] for service [{}] using algorithm [{}]",
                selectedInstance.id(),
                selectedInstance.uri(),
                service.name(),
                algorithm
        );

        return selectedInstance;
    }
}