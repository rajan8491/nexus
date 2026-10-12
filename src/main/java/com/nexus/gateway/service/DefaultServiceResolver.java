package com.nexus.gateway.service;

import com.nexus.gateway.domain.ServiceDefinition;
import com.nexus.gateway.exception.NoAvailableInstanceException;
import com.nexus.gateway.exception.ServiceNotFoundException;
import com.nexus.gateway.registry.GatewayRegistry;
import org.springframework.stereotype.Component;

@Component
public class DefaultServiceResolver implements ServiceResolver {

    private final GatewayRegistry gatewayRegistry;

    public DefaultServiceResolver(GatewayRegistry gatewayRegistry) {
        this.gatewayRegistry = gatewayRegistry;
    }

    @Override
    public ServiceDefinition resolve(String serviceName) {

        ServiceDefinition serviceDefinition = gatewayRegistry.getService(serviceName)
                .orElseThrow(() -> new ServiceNotFoundException(serviceName));

        if (serviceDefinition.instances().isEmpty()) {
            throw new NoAvailableInstanceException("No instance found for service " + serviceName);
        }

        return serviceDefinition;
    }
}