package com.nexus.gateway.loadbalancer;

import com.nexus.gateway.domain.ServiceDefinition;
import com.nexus.gateway.domain.ServiceInstance;

public interface LoadBalancer {
    ServiceInstance choose(ServiceDefinition service);
}