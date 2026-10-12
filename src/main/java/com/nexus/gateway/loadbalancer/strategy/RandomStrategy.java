package com.nexus.gateway.loadbalancer.strategy;

import com.nexus.gateway.domain.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomStrategy implements LoadBalancerStrategy {

    @Override
    public String getAlgorithm() {
        return "RANDOM";
    }

    @Override
    public ServiceInstance choose(List<ServiceInstance> healthyInstances) {
        int index = ThreadLocalRandom.current().nextInt(healthyInstances.size());
        return healthyInstances.get(index);
    }
}