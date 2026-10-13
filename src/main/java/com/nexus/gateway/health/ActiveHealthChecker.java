package com.nexus.gateway.health;

import com.nexus.gateway.domain.HealthCheckConfig;
import com.nexus.gateway.domain.ServiceInstance;
import com.nexus.gateway.registry.GatewayRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ActiveHealthChecker {

    private final GatewayRegistry registry;
    private final WebClient webClient;
    private final Set<String> unhealthyInstanceIds = ConcurrentHashMap.newKeySet();

    public ActiveHealthChecker(GatewayRegistry registry, WebClient webClient) {
        this.registry = registry;
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        HealthCheckConfig config = registry.getHealthCheckConfig();
        if (config == null || !config.enabled()) {
            return;
        }

        Flux.interval(Duration.ofSeconds(1), Duration.ofSeconds(config.intervalSeconds()))
                .flatMap(tick -> checkAllInstances())
                .subscribe();
    }

    private Flux<Void> checkAllInstances() {
        HealthCheckConfig config = registry.getHealthCheckConfig();

        return Flux.fromIterable(registry.getAllServices())
                .flatMap(service -> Flux.fromIterable(service.instances()))
                .flatMap(instance ->
                        webClient.get()
                                .uri(instance.uri().resolve(config.path()))
                                .retrieve()
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(config.timeoutMillis()))
                                .map(res -> res.getStatusCode().is2xxSuccessful())
                                .onErrorReturn(false)
                                .doOnNext(isHealthy -> {
                                    if (isHealthy) {
                                        unhealthyInstanceIds.remove(instance.id());
                                    } else {
                                        unhealthyInstanceIds.add(instance.id());
                                        log.warn("Instance [{}] at uri {} is down", instance.id(), instance.uri());
                                    }
                                })
                                .then()
                );
    }

    public boolean isHealthy(ServiceInstance instance) {
        return !unhealthyInstanceIds.contains(instance.id());
    }
}