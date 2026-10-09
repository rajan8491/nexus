package com.nexus.gateway.service;

import java.util.List;

public record ServiceDefinition(

        List<ServiceInstance> instances

) {
}