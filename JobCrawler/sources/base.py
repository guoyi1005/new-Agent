from abc import ABC, abstractmethod
from dataclasses import dataclass
import random
import time

import httpx

from config import MAX_DELAY, MIN_DELAY, USER_AGENT


@dataclass
class JobStub:
    source_job_id: str
    source_url: str
    job_title: str | None = None
    company_name: str | None = None
    city: str | None = None
    salary_text: str | None = None
    education: str | None = None


class SourceStopped(RuntimeError):
    def __init__(self, status: str, message: str):
        super().__init__(message)
        self.status = status


class HttpClient:
    def __init__(self):
        self.client = httpx.Client(
            headers={"User-Agent": USER_AGENT, "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.5"},
            timeout=httpx.Timeout(20, connect=10), follow_redirects=True,
            max_redirects=3,
        )
        self.last_request_at: float | None = None

    def get(self, url: str) -> str:
        if self.last_request_at is not None:
            time.sleep(random.uniform(MIN_DELAY, MAX_DELAY))
        self.last_request_at = time.monotonic()
        for attempt in range(3):
            try:
                response = self.client.get(url)
            except httpx.HTTPError as exc:
                if attempt == 2:
                    raise SourceStopped("UNAVAILABLE", f"HTTP request failed: {exc}") from exc
                time.sleep(1.0 + attempt)
                continue
            # Do not retain even anonymous Set-Cookie responses between requests.
            self.client.cookies.clear()
            if response.status_code in (403, 429):
                raise SourceStopped("BLOCKED", f"source returned HTTP {response.status_code}; no retry")
            if response.status_code >= 500:
                if attempt < 2:
                    time.sleep(1.0 + attempt)
                    continue
                raise SourceStopped("UNAVAILABLE", f"source returned HTTP {response.status_code}")
            if response.status_code >= 400:
                raise SourceStopped("UNAVAILABLE", f"source returned HTTP {response.status_code}")
            body = response.text
            lower = body.lower()
            if any(marker in lower for marker in ("访问验证", "滑动验证", "security verification", "请先登录")):
                raise SourceStopped("BLOCKED", "verification or login page detected; no retry")
            return body
        raise SourceStopped("UNAVAILABLE", "request retry limit reached")

    def close(self):
        self.client.close()


class JobSourceProvider(ABC):
    source_name: str

    @abstractmethod
    def discover_jobs(self, keyword: str, limit: int) -> list[JobStub]:
        raise NotImplementedError

    @abstractmethod
    def fetch_detail(self, job_stub: JobStub):
        raise NotImplementedError
