package com.integration.client;

import com.integration.model.Customer;
import com.integration.service.CRMClient;
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
class CRMClientTest {

    @Test
    void shouldFetchCustomersSuccessfully() {

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server =
                MockRestServiceServer.createServer(restTemplate);

        CRMClient client = new CRMClient(restTemplate);

        ReflectionTestUtils.setField(
                client,
                "baseUrl",
                "http://mock-api:4010"
        );

        server.expect(requestTo("http://mock-api:4010/customers"))
                .andRespond(withSuccess("""
                        [
                          {"id":1,"name":"John","email":"john@test.com"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<Customer> customers = client.fetchCustomers();

        assertEquals(1, customers.size());
        assertEquals("John", customers.get(0).getName());

        server.verify();
    }
}
