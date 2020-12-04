package com.dolittle.ecom.app;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "customer")
public @Data class CustomerConfig {
    private Map<String, String> pgiProviders;
    private String productFilters;
}
