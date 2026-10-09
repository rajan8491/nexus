package com.nexus.gateway.loadbalancer;

import com.nexus.gateway.service.ServiceDefinition;
import com.nexus.gateway.service.ServiceInstance;

public interface LoadBalancer {
    ServiceInstance choose(ServiceDefinition service);
}
