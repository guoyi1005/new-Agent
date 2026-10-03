from datetime import datetime, timezone
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, HttpUrl


def now_iso() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


class RawJobData(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    source: str
    source_job_id: Optional[str] = None
    source_url: HttpUrl
    job_title: str
    company_name: Optional[str] = None
    city: Optional[str] = None
    district: Optional[str] = None
    salary_text: Optional[str] = None
    education: Optional[str] = None
    experience: Optional[str] = None
    job_type: Optional[str] = None
    description: Optional[str] = None
    requirements: Optional[str] = None
    company_industry: Optional[str] = None
    company_size: Optional[str] = None
    publish_time: Optional[str] = None
    is_active: bool = True
    is_recent: bool = False
    crawl_time: str = Field(default_factory=now_iso)


class ProcessedJob(RawJobData):
    normalized_title: str
    salary_min: Optional[float] = None
    salary_max: Optional[float] = None
    salary_unit: Optional[str] = None
    salary_months: Optional[int] = None
    skills: list[str] = Field(default_factory=list)
    fingerprint: str
    content_hash: str
    quality_score: int
    last_seen_time: str = Field(default_factory=now_iso)
