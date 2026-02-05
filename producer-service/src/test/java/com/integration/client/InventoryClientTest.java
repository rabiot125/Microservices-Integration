package com.integration.client;

import com.integration.model.Product;
import com.integration.service.InventoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class InventoryClientTest {

    @Test
    void shouldFetchProductsSuccessfully() {

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server =
                MockRestServiceServer.createServer(restTemplate);

        InventoryClient client = new InventoryClient(restTemplate);
        ReflectionTestUtils.setField(
                client,
                "baseUrl",
                "http://mock-api:4010"
        );

        server.expect(requestTo("http://mock-api:4010/products"))
                .andRespond(withSuccess("""
                        [
                          {"productId":100,"name":"Laptop","stock":20}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<Product> products = client.fetchProducts();

        assertEquals(1, products.size());
        assertEquals("Laptop", products.get(0).getName());

        server.verify();
    }
}
