package com.integration.producer;

import com.integration.config.KafkaTopics;
import com.integration.model.Customer;
import com.integration.model.Product;
import com.integration.service.CRMClient;
import com.integration.service.InventoryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaPublishService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CRMClient crmClient;
    private final InventoryClient inventoryClient;

    public KafkaPublishService(KafkaTemplate<String, Object> kafkaTemplate,
                               CRMClient crmClient,
                               InventoryClient inventoryClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.crmClient = crmClient;
        this.inventoryClient = inventoryClient;
    }

    public void publishAll() {

        log.info("Starting publish job...");

        // ---------------- Customers ----------------
        for (Customer c : crmClient.fetchCustomers()) {

            String key = (c.getId() != null)
                    ? c.getId().toString()
                    : "unknown-" + System.currentTimeMillis();

            kafkaTemplate.send(KafkaTopics.CUSTOMER_TOPIC, key, c)
                    .thenAccept(result ->
                            log.info("Customer {} delivered successfully to partition={} offset={}",
                                    key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            )
                    )
                    .exceptionally(ex -> {
                        log.error("Customer {} delivery failed", key, ex);
                        return null;
                    });
        }

        // ---------------- Products ----------------
        for (Product p : inventoryClient.fetchProducts()) {

            String key = (p.getProductId() != null)
                    ? p.getProductId().toString()
                    : "unknown-" + System.currentTimeMillis();

            kafkaTemplate.send(KafkaTopics.INVENTORY_TOPIC, key, p)
                    .thenAccept(result ->
                            log.info("Product {} delivered successfully to partition={} offset={}",
                                    key,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            )
                    )
                    .exceptionally(ex -> {
                        log.error("Product {} delivery failed", key, ex);
                        return null;
                    });
        }

        log.info("Publish job completed.");
    }
}
