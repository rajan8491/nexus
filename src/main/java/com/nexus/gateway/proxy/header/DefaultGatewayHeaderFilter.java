package com.nexus.gateway.proxy.header;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultGatewayHeaderFilter implements GatewayHeaderFilter {

    // Standard HTTP hop-by-hop headers that must not be forwarded
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade"
    );

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public HttpHeaders filterRequestHeaders(ServerWebExchange exchange) {
        HttpHeaders originalHeaders = exchange.getRequest().getHeaders();
        HttpHeaders filtered = new HttpHeaders();

        // 1. Remove hop-by-hop headers and dynamically check Connection-named headers
        List<String> connectionTokens = originalHeaders.getConnection();

        originalHeaders.forEach((name, values) -> {
            String lowerName = name.toLowerCase();

            boolean isHopByHop = HOP_BY_HOP_HEADERS.contains(lowerName) || connectionTokens.contains(lowerName);
            boolean isRestrictedHeader = HttpHeaders.HOST.equalsIgnoreCase(name)
                    || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name);

            if (!isHopByHop && !isRestrictedHeader) {
                filtered.put(name, values);
            }
        });

        // 2. Prevent spoofed X-Forwarded-* propagation (strip and overwrite safely)
        filtered.remove("X-Forwarded-For");
        filtered.remove("X-Forwarded-Host");
        filtered.remove("X-Forwarded-Port");
        filtered.remove("X-Forwarded-Proto");
        filtered.remove("Forwarded");

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            filtered.add("X-Forwarded-For", remoteAddress.getAddress().getHostAddress());
        }

        filtered.add("X-Forwarded-Host", exchange.getRequest().getURI().getHost());

        int port = exchange.getRequest().getURI().getPort();
        if (port != -1) {
            filtered.add("X-Forwarded-Port", String.valueOf(port));
        }
        filtered.add("X-Forwarded-Proto", exchange.getRequest().getURI().getScheme());

        // 3. Generate or propagate Correlation ID
        String correlationId = originalHeaders.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        filtered.add(CORRELATION_ID_HEADER, correlationId);

        return filtered;
    }

    @Override
    public HttpHeaders filterResponseHeaders(HttpHeaders originalResponseHeaders) {
        HttpHeaders filtered = new HttpHeaders();
        List<String> connectionTokens = originalResponseHeaders.getConnection();

        // Separate response filtering to strip outbound hop-by-hop headers
        originalResponseHeaders.forEach((name, values) -> {
            String lowerName = name.toLowerCase();
            boolean isHopByHop = HOP_BY_HOP_HEADERS.contains(lowerName) || connectionTokens.contains(lowerName);

            if (!isHopByHop) {
                filtered.put(name, values);
            }
        });

        return filtered;
    }
}