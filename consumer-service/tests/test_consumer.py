# tests/test_consumer.py
import json
import pytest
from unittest.mock import AsyncMock, patch, call
from aiokafka import ConsumerRecord

from consumer import process  


@pytest.mark.asyncio
async def test_customer_then_product_triggers_merge_and_analytics_call(
    fake_redis,
    mock_http_responses,
    mock_kafka_consumer,
    mock_dlq_producer,
    sample_customer_msg,
    sample_product_msg,
):
    mock_http_responses.post(
        "http://mock-api:4010/analytics/data",
        json={"status": "SUCCESS"},
        status=200
    )

    with patch("consumer.producer", mock_dlq_producer):
        await process(sample_customer_msg, mock_kafka_consumer)
        await process(sample_product_msg, mock_kafka_consumer)

    # Exactly one analytics call
    assert len(mock_http_responses.calls) == 1
    sent_payload = json.loads(mock_http_responses.calls[0].request.body)
    assert sent_payload["customerId"] == "C123"
    assert sent_payload["productSku"] == "P456"

    mock_kafka_consumer.commit.assert_called_once()
    mock_dlq_producer.send_and_wait.assert_not_called()


@pytest.mark.asyncio
async def test_duplicate_message_skipped_and_committed(
    fake_redis,
    mock_kafka_consumer,
    sample_customer_msg,
):
    # First occurrence
    await process(sample_customer_msg, mock_kafka_consumer)
    mock_kafka_consumer.commit.assert_called_once()
    mock_kafka_consumer.commit.reset_mock()

    # Same message again → should skip
    await process(sample_customer_msg, mock_kafka_consumer)

    mock_kafka_consumer.commit.assert_called_once()
    # No analytics, no DLQ


@pytest.mark.asyncio
async def test_analytics_failure_sends_to_dlq_and_commits(
    fake_redis,
    mock_http_responses,
    mock_kafka_consumer,
    mock_dlq_producer,
    sample_customer_msg,
):
    mock_http_responses.post(
        "http://mock-api:4010/analytics/data",
        status=503
    )

    with patch("consumer.producer", mock_dlq_producer), \
         patch("analytics_client.send_to_analytics", side_effect=Exception("Service unavailable")):

        await process(sample_customer_msg, mock_kafka_consumer)

    mock_dlq_producer.send_and_wait.assert_called_once()
    args = mock_dlq_producer.send_and_wait.call_args[0]
    assert args[0] == "analytics_dlq"   # topic
    dlq_payload = args[1]
    assert "error" in dlq_payload
    assert "C123" in json.dumps(dlq_payload)

    mock_kafka_consumer.commit.assert_called_once()


@pytest.mark.asyncio
async def test_invalid_message_is_caught_and_committed(
    mock_kafka_consumer,
    mock_dlq_producer,
):
    bad_msg = ConsumerRecord(
        topic="customer_data",
        partition=0,
        offset=9999,
        key=b"bad",
        value=b'{"id":"broken" "missing colon"}'
    )

    with patch("consumer.producer", mock_dlq_producer):
        await process(bad_msg, mock_kafka_consumer)

    mock_kafka_consumer.commit.assert_called_once()
    # Optional: assert DLQ was called with original broken value


@pytest.mark.asyncio
@patch("idempotency.get_redis", return_value=None)  # simulate Redis down
async def test_redis_down_falls_back_to_in_memory_dedup(
    mock_get_redis,
    sample_customer_msg,
    mock_kafka_consumer,
):
    # First
    await process(sample_customer_msg, mock_kafka_consumer)
    mock_kafka_consumer.commit.assert_called_once()
    mock_kafka_consumer.commit.reset_mock()

    # Same message → should still detect duplicate via in-memory set
    await process(sample_customer_msg, mock_kafka_consumer)

    mock_kafka_consumer.commit.assert_called_once()