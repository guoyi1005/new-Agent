from urllib.parse import urlencode

from config import MAX_PER_KEYWORD
from parsers.shixiseng_parser import ShixisengParser
from sources.base import HttpClient, JobSourceProvider, JobStub, SourceStopped


class ShixisengSource(JobSourceProvider):
    source_name = "shixiseng"
    LIST_URL = "https://s.shixiseng.com/interns"
    BASE_URL = "https://www.shixiseng.com"

    def __init__(self):
        self.http = HttpClient()
        self.parser = ShixisengParser()
        self.status = "READY"

    def discover_jobs(self, keyword: str, limit: int) -> list[JobStub]:
        query = urlencode({"from": "menu", "keyword": keyword})
        try:
            html = self.http.get(f"{self.LIST_URL}?{query}")
            return self.parser.parse_list(html, self.BASE_URL)[:max(1, min(limit, MAX_PER_KEYWORD))]
        except (SourceStopped, ValueError) as exc:
            self.status = exc.status if isinstance(exc, SourceStopped) else "BLOCKED"
            raise

    def fetch_detail(self, job_stub: JobStub):
        try:
            return self.parser.parse_detail(self.http.get(job_stub.source_url), job_stub)
        except SourceStopped as exc:
            self.status = exc.status
            raise

    def close(self):
        self.http.close()
