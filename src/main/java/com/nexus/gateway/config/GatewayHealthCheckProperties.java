package com.nexus.gateway.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GatewayHealthCheckProperties(

        boolean enabled,

        @Min(value = 1, message = "health-check.interval-seconds must be at least 1 second")
        int intervalSeconds,

        @Min(value = 100, message = "health-check.timeout-millis must be at least 100ms")
        int timeoutMillis,

        @NotBlank(message = "health-check.path must not be blank")
        @Pattern(regexp = "^/.*", message = "health-check.path must start with '/'")
        String path

) {
    public GatewayHealthCheckProperties {
        if (intervalSeconds == 0) intervalSeconds = 10;
        if (timeoutMillis == 0) timeoutMillis = 2000;
        if (path == null || path.isBlank()) path = "/actuator/health";
    }
}