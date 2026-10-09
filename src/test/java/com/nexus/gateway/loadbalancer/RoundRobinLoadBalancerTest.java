package com.nexus.gateway.loadbalancer;

import com.nexus.gateway.exception.NoAvailableInstanceException;
import com.nexus.gateway.service.ServiceDefinition;
import com.nexus.gateway.service.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoundRobinLoadBalancerTest {

    @Test
    void shouldSelectInstancesInRoundRobinOrder() {

        ServiceDefinition service =
                new ServiceDefinition(
                        List.of(
                                new ServiceInstance(
                                        "user-1",
                                        URI.create("http://localhost:8081")
                                ),
                                new ServiceInstance(
                                        "user-2",
                                        URI.create("http://localhost:8082")
                                ),
                                new ServiceInstance(
                                        "user-3",
                                        URI.create("http://localhost:8083")
                                )
                        )
                );

        LoadBalancer loadBalancer =
                new RoundRobinLoadBalancer();

        assertEquals(
                "user-1",
                loadBalancer.choose(service).id()
        );

        assertEquals(
                "user-2",
                loadBalancer.choose(service).id()
        );

        assertEquals(
                "user-3",
                loadBalancer.choose(service).id()
        );

        assertEquals(
                "user-1",
                loadBalancer.choose(service).id()
        );
    }

    @Test
    void shouldRejectServiceWithoutInstances() {

        ServiceDefinition service =
                new ServiceDefinition(List.of());

        LoadBalancer loadBalancer =
                new RoundRobinLoadBalancer();

        assertThrows(
                NoAvailableInstanceException.class,
                () -> loadBalancer.choose(service)
        );
    }
}