package com.nexus.gateway.loadbalancer;

import com.nexus.gateway.exception.NoAvailableInstanceException;
import com.nexus.gateway.service.ServiceDefinition;
import com.nexus.gateway.service.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinLoadBalancer
        implements LoadBalancer {

    private final AtomicInteger counter =
            new AtomicInteger();

    @Override
    public ServiceInstance choose(
            ServiceDefinition service
    ) {

        List<ServiceInstance> instances =
                service.instances();

        if (instances == null || instances.isEmpty()) {
            throw new NoAvailableInstanceException();
        }

        int index = Math.floorMod(
                counter.getAndIncrement(),
                instances.size()
        );

        return instances.get(index);
    }
}