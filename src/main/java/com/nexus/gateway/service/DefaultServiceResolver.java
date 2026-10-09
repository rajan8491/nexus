package com.nexus.gateway.service;

import com.nexus.gateway.config.GatewayProperties;
import com.nexus.gateway.config.GatewayServiceInstanceProperties;
import com.nexus.gateway.config.GatewayServiceProperties;
import com.nexus.gateway.exception.NoAvailableInstanceException;
import com.nexus.gateway.exception.ServiceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultServiceResolver
        implements ServiceResolver {

    private final GatewayProperties gatewayProperties;

    public DefaultServiceResolver(
            GatewayProperties gatewayProperties
    ) {
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public ServiceDefinition resolve(
            String serviceName
    ) {

        GatewayServiceProperties serviceProperties =
                gatewayProperties.services()
                        .get(serviceName);
        if (serviceProperties == null) {
            throw new ServiceNotFoundException(serviceName);
        }

        List<ServiceInstance> serviceInstances =
                serviceProperties.instances()
                        .stream()
                        .map(this::toServiceInstance)
                        .toList();

        if (serviceInstances.isEmpty()) {
            throw new NoAvailableInstanceException("No instance found for service " + serviceName);
        }

        return new ServiceDefinition(serviceInstances);
    }

    private ServiceInstance toServiceInstance(GatewayServiceInstanceProperties serviceInstanceProperties) {
        return new ServiceInstance(
                serviceInstanceProperties.id(),
                serviceInstanceProperties.uri()
        );
    }
}