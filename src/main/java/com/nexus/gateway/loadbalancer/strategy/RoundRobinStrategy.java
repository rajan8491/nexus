package com.nexus.gateway.loadbalancer.strategy;

import com.nexus.gateway.domain.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinStrategy implements LoadBalancerStrategy {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String getAlgorithm() {
        return "ROUND_ROBIN";
    }

    @Override
    public ServiceInstance choose(List<ServiceInstance> healthyInstances) {
        int index = Math.floorMod(counter.getAndIncrement(), healthyInstances.size());
        return healthyInstances.get(index);
    }
}