package com.integration.service;

import com.integration.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@Slf4j
public class InventoryClient {

    @Value("${mock.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public InventoryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retry(name = "inventoryRetry")
    @CircuitBreaker(name = "inventoryCircuit", fallbackMethod = "fallbackProducts")
    public List<Product> fetchProducts() {

        String url = baseUrl + "/products";

        log.info("Fetching products from Inventory: {}", url);

        Product[] arr =
                restTemplate.getForObject(url, Product[].class);

        return Arrays.asList(arr);
    }

    /**
     * Fallback executed when Inventory API fails repeatedly
     */
    public List<Product> fallbackProducts(Throwable ex) {
        log.error("Inventory fetch failed, returning empty list", ex);
        return List.of();
    }
}

