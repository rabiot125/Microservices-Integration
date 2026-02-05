package com.integration.service;

import com.integration.model.Customer;
import com.integration.model.Product;
import com.integration.producer.KafkaPublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPublishServiceTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private CRMClient crmClient;
    @Mock
    private InventoryClient inventoryClient;
    @InjectMocks
    private KafkaPublishService kafkaPublishService;
    @Test
    void shouldPublishAllCustomersAndProducts_withCorrectKeys() {
        // Arrange
        var customers = List.of(
                new Customer(1L, "John", "john@test.com"),
                new Customer(2L, "Mary", "mary@test.com")
        );
        var products = List.of(
                new Product(100L, "Laptop", 50),
                new Product(200L, "Phone", 100)
        );

        when(crmClient.fetchCustomers()).thenReturn(customers);
        when(inventoryClient.fetchProducts()).thenReturn(products);

        // Act
        kafkaPublishService.publishAll();

        // Assert - customers
        ArgumentCaptor<String> customerKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(
                eq("customer_data"),
                customerKeyCaptor.capture(),
                any(Customer.class)
        );
        assertThat(customerKeyCaptor.getAllValues())
                .containsExactly("1", "2");

        // Assert - products
        ArgumentCaptor<String> productKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(
                eq("inventory_data"),
                productKeyCaptor.capture(),
                any(Product.class)
        );
        assertThat(productKeyCaptor.getAllValues())
                .containsExactly("100", "200");
    }

    @Test
    void shouldGenerateFallbackKey_whenCustomerIdIsNull() {
        // Arrange
        var ghost = new Customer(null, "Ghost", "ghost@test.com");
        when(crmClient.fetchCustomers()).thenReturn(List.of(ghost));
        when(inventoryClient.fetchProducts()).thenReturn(List.of());

        // Act
        kafkaPublishService.publishAll();

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("customer_data"),
                keyCaptor.capture(),
                eq(ghost)
        );

        assertThat(keyCaptor.getValue())
                .startsWith("unknown-")
                .matches("unknown-\\d+");
    }

    @Test
    void shouldNotPublishOrFail_whenNoDataReturned() {
        // Arrange
        when(crmClient.fetchCustomers()).thenReturn(List.of());
        when(inventoryClient.fetchProducts()).thenReturn(List.of());

        // Act & Assert
        assertDoesNotThrow(() -> kafkaPublishService.publishAll());

        verifyNoInteractions(kafkaTemplate);  // even better than never()
    }

    @Test
    void shouldHandleNullListFromClient_withoutNPE() {
        // Arrange - simulate buggy client returning null
        when(crmClient.fetchCustomers()).thenReturn(null);
        when(inventoryClient.fetchProducts()).thenReturn(List.of());

        // Act & Assert
        assertDoesNotThrow(() -> kafkaPublishService.publishAll());

        verifyNoInteractions(kafkaTemplate);
    }
}