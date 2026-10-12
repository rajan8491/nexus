package com.nexus.gateway.loadbalancer.strategy;

import com.nexus.gateway.domain.ServiceInstance;

import java.util.List;

public interface LoadBalancerStrategy {
    String getAlgorithm();

    ServiceInstance choose(List<ServiceInstance> instances);
}