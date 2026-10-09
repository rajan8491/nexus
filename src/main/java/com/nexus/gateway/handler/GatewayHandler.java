package com.nexus.gateway.handler;

import com.nexus.gateway.loadbalancer.LoadBalancer;
import com.nexus.gateway.proxy.RequestForwarder;
import com.nexus.gateway.proxy.TargetUriBuilder;
import com.nexus.gateway.route.RouteDefinition;
import com.nexus.gateway.route.RouteResolver;
import com.nexus.gateway.service.ServiceDefinition;
import com.nexus.gateway.service.ServiceInstance;
import com.nexus.gateway.service.ServiceResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class GatewayHandler {

    private final RouteResolver routeResolver;
    private final ServiceResolver serviceResolver;
    private final LoadBalancer loadBalancer;
    private final RequestForwarder requestForwarder;
    private final TargetUriBuilder targetUriBuilder;

    public GatewayHandler(
            RouteResolver routeResolver,
            ServiceResolver serviceResolver,
            LoadBalancer loadBalancer,
            RequestForwarder requestForwarder, TargetUriBuilder targetUriBuilder) {
        this.routeResolver = routeResolver;
        this.serviceResolver = serviceResolver;
        this.loadBalancer = loadBalancer;
        this.requestForwarder = requestForwarder;
        this.targetUriBuilder = targetUriBuilder;
    }

    public Mono<ResponseEntity<byte[]>> handle(
            ServerWebExchange exchange
    ) {

        String path = exchange
                .getRequest()
                .getPath()
                .value();

        RouteDefinition route =
                routeResolver.resolve(path);

        ServiceDefinition service =
                serviceResolver.resolve(
                        route.service()
                );

        ServiceInstance instance =
                loadBalancer.choose(service);

        URI targetUri =
                targetUriBuilder.build(
                        instance.uri(),
                        exchange
                );

        return requestForwarder.forward(
                exchange,
                targetUri
        );

//        return Mono.just(
//                ResponseEntity.ok(
//                        ("Forward to " + targetUri)
//                                .getBytes()
//                )
//        );
    }
}
