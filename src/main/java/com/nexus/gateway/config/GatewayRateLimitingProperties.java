package com.nexus.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record GatewayRateLimitingProperties(

        boolean enabled,

        @NotBlank(message = "gateway.rate-limiting.default-algorithm must be provided")
        String defaultAlgorithm,

        List<@Valid RateLimitRuleProperties> rules

) {
    public GatewayRateLimitingProperties {
        if (defaultAlgorithm == null || defaultAlgorithm.isBlank()) {
            defaultAlgorithm = "TOKEN_BUCKET";
        }
        if (rules == null) {
            rules = List.of();
        }
    }

    public record RateLimitRuleProperties(

            @NotBlank(message = "Rate limit rule id must not be blank")
            String id,

            @NotBlank(message = "Rate limit rule path must not be blank")
            @Pattern(regexp = "^/.*", message = "Rate limit rule path must start with '/'")
            String path,

            String method, // Optional: GET, POST, or null for ALL

            @NotNull(message = "key-type must be specified (IP or HEADER)")
            KeyType keyType,

            String headerName,

            @NotBlank(message = "Rule algorithm must not be blank")
            String algorithm,

            @Min(value = 1, message = "capacity must be at least 1")
            int capacity,

            int refillRate,

            int windowSeconds

    ) {
        public enum KeyType {
            IP, HEADER
        }
    }
}