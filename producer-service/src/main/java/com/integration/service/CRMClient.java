package com.integration.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import  com.integration.model.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@Slf4j

public class CRMClient {

    @Value("${mock.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public CRMClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retry(name = "crmRetry")
    @CircuitBreaker(name = "crmCircuit", fallbackMethod = "fallbackCustomers")
    public List<Customer> fetchCustomers() {

        String url = baseUrl + "/customers";

        log.info("Fetching customers from CRM: {}", url);

        Customer[] arr =
                restTemplate.getForObject(url, Customer[].class);

        return Arrays.asList(arr);
    }

    /**
     * Fallback executed when retries fail OR circuit breaker is open.
     */
    public List<Customer> fallbackCustomers(Throwable ex) {
        log.error("CRM fetch failed, returning empty list", ex);
        return List.of();
    }
}
