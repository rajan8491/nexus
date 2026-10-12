package com.nexus.gateway.service;

import com.nexus.gateway.domain.ServiceDefinition;

public interface ServiceResolver {
    ServiceDefinition resolve(String serviceName);
}
