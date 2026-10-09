package com.nexus.gateway.service;

import jakarta.validation.constraints.NotBlank;

import java.net.URI;

public record ServiceInstance(

        @NotBlank
        String id,

        @NotBlank
        URI uri

) {
}