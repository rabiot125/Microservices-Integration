import os
import json
import asyncio
import logging
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from prometheus_client import Counter, start_http_server

from merger import merge
from analytics_client import send_to_analytics
from idempotency import is_duplicate

# ---- CONFIG --- #

KAFKA = os.getenv("KAFKA_BOOTSTRAP", "kafka:29092")
BASE = os.getenv("MOCK_API_URL", "http://mock-api:4010")

CUSTOMER_TOPIC = "customer_data"
INVENTORY_TOPIC = "inventory_data"
DLQ_TOPIC = "analytics_dlq"

# --- LOGGING ----- #

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s"
)
log = logging.getLogger("consumer")

# ---METRICS ----- #

processed = Counter("messages_processed", "Total processed messages")
failed = Counter("messages_failed", "Total failed messages")

start_http_server(9000)

# ------- CACHE ------ #

customer_cache = {}
product_cache = {}

# ---- DLQ PRODUCER ----- #

producer = None


async def init_dlq():
    global producer
    producer = AIOKafkaProducer(
        bootstrap_servers=KAFKA,
        value_serializer=lambda v: json.dumps(v).encode()
    )
    await producer.start()


async def send_dlq(payload):
    await producer.send_and_wait(DLQ_TOPIC, payload)
    log.warning("Sent payload to DLQ")


# ---- MESSAGE HANDLER ----- #

async def process(msg, consumer):

    try:
        data = json.loads(msg.value.decode())

        if is_duplicate(data):
            log.info("Skipping duplicate message")
            return

        key = data.get("id")

        if msg.topic == CUSTOMER_TOPIC:
            customer_cache[key] = data

        elif msg.topic == INVENTORY_TOPIC:
            product_cache[key] = data

        # Merge only when both exist
        if key in customer_cache and key in product_cache:
            merged = merge(customer_cache[key], product_cache[key])

            try:
                await send_to_analytics(merged, BASE)

                processed.inc()
                log.info(f"Sent merged payload for key={key}")

                # Cleanup cache
                customer_cache.pop(key)
                product_cache.pop(key)

                await consumer.commit()

            except Exception as e:
                failed.inc()
                log.error(f"Analytics failed: {e}")
                await send_dlq(merged)

    except Exception as e:
        failed.inc()
        log.exception("Processing error")


async def main():

    log.info(f"Connecting to Kafka at {KAFKA}")

    consumer = AIOKafkaConsumer(
        CUSTOMER_TOPIC,
        INVENTORY_TOPIC,
        bootstrap_servers=KAFKA,
        group_id="enterprise-consumer",
        enable_auto_commit=False
    )

    await consumer.start()
    await init_dlq()

    log.info("Consumer running successfully")

    try:
        async for msg in consumer:
            asyncio.create_task(process(msg, consumer))

    except KeyboardInterrupt:
        log.warning("Shutdown requested")

    finally:
        log.info("Closing consumer...")
        await consumer.stop()
        await producer.stop()


if __name__ == "__main__":
    asyncio.run(main())
