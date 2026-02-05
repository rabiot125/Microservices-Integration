import hashlib
import redis
import os

REDIS_HOST = os.getenv("REDIS_HOST", "redis")

r = redis.Redis(host=REDIS_HOST, port=6379, decode_responses=True)

def is_duplicate(record):

    key = hashlib.sha256(str(record).encode()).hexdigest()

    if r.exists(key):
        return True

    r.set(key, "1", ex=3600)  # 1 hour TTL
    return False
