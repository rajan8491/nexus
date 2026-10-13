package com.nexus.gateway.loadbalancer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nexus.gateway.config.GatewayProperties;
import com.nexus.gateway.domain.ServiceDefinition;
import com.nexus.gateway.domain.ServiceInstance;
import com.nexus.gateway.health.ActiveHealthChecker;
import com.nexus.gateway.loadbalancer.strategy.LoadBalancerStrategy;
import com.nexus.gateway.registry.GatewayRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartLoadBalancerTest {

    @Test
    void shouldLogSelectedServiceInstance() {
        Logger logger = (Logger) LoggerFactory.getLogger(SmartLoadBalancer.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ServiceInstance first = new ServiceInstance("instance-1", URI.create("http://localhost:8081"));
            ServiceInstance selectedInstance = new ServiceInstance("instance-2", URI.create("http://localhost:8082"));

            SmartLoadBalancer loadBalancer = new SmartLoadBalancer(
                    createHealthChecker(),
                    List.of(new SelectingStrategy(selectedInstance))
            );

            ServiceDefinition service = new ServiceDefinition(
                    "inventory-service",
                    "TEST",
                    List.of(first, selectedInstance)
            );

            ServiceInstance result = loadBalancer.choose(service);

            assertEquals(selectedInstance, result);
            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.get(0);
            assertEquals("INFO", event.getLevel().toString());
            assertTrue(event.getFormattedMessage().contains("inventory-service"));
            assertTrue(event.getFormattedMessage().contains("instance-2"));
            assertTrue(event.getFormattedMessage().contains("http://localhost:8082"));
            assertTrue(event.getFormattedMessage().contains("TEST"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private ActiveHealthChecker createHealthChecker() {
        GatewayProperties properties = new GatewayProperties(
                null,
                null,
                null,
                null,
                null,
                List.of(),
                Map.of()
        );

        GatewayRegistry registry = new GatewayRegistry(properties);
        return new ActiveHealthChecker(registry, WebClient.create());
    }

    private record SelectingStrategy(ServiceInstance selectedInstance) implements LoadBalancerStrategy {

        @Override
        public String getAlgorithm() {
            return "TEST";
        }

        @Override
        public ServiceInstance choose(List<ServiceInstance> instances) {
            return selectedInstance;
        }
    }
}

