# Production-Ready Integration Pipeline

Full enterprise-grade integration system:

- Java Spring Boot Producers (CRM + Inventory)
- Kafka Topics: customer_data, inventory_data
- Python Consumers with Merge + Retry + Idempotency
- Dead Letter Queue (analytics_DLQ)
- Prometheus Metrics (port 9000)

## Run Everything

```bash
docker-compose up --build
```

## Expected Flow
1. Producers fetch /customers and /products every minute
2. Publish events into Kafka topics
3. Python consumer merges and POSTs to Analytics
4. Failures go to DLQ

## Monitoring
Prometheus scrape endpoint:
http://localhost:9000


## Sample Output (Successful Run + Idempotency Proof)
mock-api-1          | [6:47:17 PM] ›     [NEGOTIATOR] ✔  success   Found a compatible content for application/json, application/*+json
mock-api-1          | [6:47:17 PM] ›     [NEGOTIATOR] ✔  success   Responding with the requested status code 200   
mock-api-1          | [6:47:17 PM] ›     [NEGOTIATOR] ℹ  info      > Responding with "200"
producer-service-1  | 2026-02-04T18:47:18.060Z  INFO 1 --- [ad | producer-1] c.i.producer.KafkaPublishService      
   : Customer 1 delivered successfully to partition=0 offset=0
producer-service-1  | 2026-02-04T18:47:18.067Z  INFO 1 --- [ad | producer-1] c.i.producer.KafkaPublishService      
   : Customer 2 delivered successfully to partition=0 offset=1
producer-service-1  | 2026-02-04T18:47:18.079Z  INFO 1 --- [   scheduling-1] c.i.producer.KafkaPublishService      
   : Publish job completed.
producer-service-1  | 2026-02-04T18:47:18.100Z  INFO 1 --- [ad | producer-1] c.i.producer.KafkaPublishService      
   : Product 100 delivered successfully to partition=0 offset=0
producer-service-1  | 2026-02-04T18:47:18.100Z  INFO 1 --- [ad | producer-1] c.i.producer.KafkaPublishService      
   : Product 200 delivered successfully to partition=0 offset=1


consumer-service-1  | 2026-02-04 18:49:14,879 | INFO | Skipping duplicate message
producer-service-1  | 2026-02-04T18:49:14.858Z  INFO 1 --- [ad | producer-1] c.i.producer.KafkaPublishService      
   : Customer 1 delivered successfully to partition=0 offset=4
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ✔  success   Found a compatible content for application/json, application/*+json


mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ✔  success   Responding with the requested status code 200   
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ✔  success   Responding with the requested status code 200   
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ℹ  info      > Responding with "200"KafkaPublishService      
mock-api-1          | [6:49:14 PM] › [HTTP SERVER] get /products ℹ  info      Request receivedaPublishService      
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ℹ  info      > Responding with "200"KafkaPublishService      
mock-api-1          | [6:49:14 PM] › [HTTP SERVER] get /products ℹ  info      Request receivedaPublishService      
mock-api-1          | [6:49:14 PM] › [HTTP SERVER] get /products ℹ  info      Request receivedaPublishService      


producer-service-1  | 2026-02-04T18:49:14.877Z  INFO 1 --- [ad | producer-1] c.i.producer.KafkaPublishService      
   : Product 200 delivered successfully to partition=0 offset=5
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ℹ  info      Request contains an accept header: application/json, application/*+json
mock-api-1          | [6:49:14 PM] ›     [VALIDATOR] ✔  success   The request passed the validation rules. Looking 
for the best response
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ✔  success   Found a compatible content for application/json, application/*+json
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ✔  success   Responding with the requested status code 200   
mock-api-1          | [6:49:14 PM] ›     [NEGOTIATOR] ℹ  info      > Responding with "200"


producer-service-1  | 2026-02-04T18:50:14.829Z  INFO 1 --- [   scheduling-1] c.i.producer.KafkaPublishService      
   : Starting publish job...
mock-api-1          | [6:50:14 PM] › [HTTP SERVER] get /customers ℹ  info      Request received
mock-api-1          | [6:50:14 PM] ›     [NEGOTIATOR] ℹ  info      Request contains an accept header: application/json, application/*+json
consumer-service-1  | 2026-02-04 18:50:14,952 | INFO | Skipping duplicate message
producer-service-1  | 2026-02-04T18:50:14.898Z  INFO 1 --- [   scheduling-1] c.integration.service.InventoryClient 
   : Fetching products from Inventory: http://mock-api:4010/products
consumer-service-1  | 2026-02-04 18:50:14,953 | INFO | Skipping duplicate messagesed the validation rules. Looking 
producer-service-1  | 2026-02-04T18:50:14.930Z  INFO 1 --- [   scheduling-1] c.i.producer.KafkaPublishService      
   : Publis

## Architecture Overview

CRM/Inventory → REST polling → Java Producers → Kafka (topics) → Python Consumers → Merge → Analytics REST
                                    └─ DLQ on failure


## Design Decisions & Scalability

- Kafka enables horizontal scaling and decouples producers/consumers
- At-least-once delivery handled via idempotent consumers
- Async polling ensures producer non-blocking behavior
- Redis-backed idempotency ensures replay safety
- Architecture supports easy onboarding of 10+ downstream systems
                                