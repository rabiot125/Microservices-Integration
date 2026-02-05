import aiohttp
from tenacity import retry, stop_after_attempt, wait_exponential


@retry(stop=stop_after_attempt(5),
       wait=wait_exponential(multiplier=2))
async def send_to_analytics(payload, base_url):

    url = f"{base_url}/analytics/data"

    async with aiohttp.ClientSession() as session:
        async with session.post(url, json=payload) as resp:
            resp.raise_for_status()
            return await resp.json()
