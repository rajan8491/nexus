package com.nexus.gateway.proxy;

import com.nexus.gateway.domain.HttpClientDefinition;
import com.nexus.gateway.registry.GatewayRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration implements DisposableBean {

    private ConnectionProvider connectionProvider;
    private LoopResources loopResources;

    @Bean
    public WebClient gatewayWebClient(GatewayRegistry registry) {
        // 1. Fetch decoupled config from the Registry
        HttpClientDefinition config = registry.getHttpClientConfig();

        // 2. Shared Netty EventLoop threads for outbound HTTP
        this.loopResources =
                LoopResources.create(
                        "gateway-client-nio",
                        LoopResources.DEFAULT_IO_WORKER_COUNT,
                        true
                );

        // 3. Fixed connection pool with lease/acquisition timeout management
        this.connectionProvider = ConnectionProvider.builder("gateway-connection-pool")
                .maxConnections(config.maxConnections())
                .pendingAcquireMaxCount(config.pendingAcquireMaxCount())
                .pendingAcquireTimeout(config.pendingAcquireTimeout())
                .maxIdleTime(config.maxIdleTime())
                .maxLifeTime(config.maxLifeTime())
                .evictInBackground(config.maxIdleTime())
                .build();

        // 4. Configure Transport & Timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .runOn(loopResources)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().toMillis())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .responseTimeout(config.responseTimeout())
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(config.readTimeout().toMillis(), TimeUnit.MILLISECONDS))
                );

        // 5. Wrap in a single WebClient instance to be reused across all requests
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public void destroy() {
        // Graceful shutdown on application exit
        if (connectionProvider != null) {
            connectionProvider.disposeLater().block();
        }
        if (loopResources != null) {
            loopResources.disposeLater().block();
        }
    }
}