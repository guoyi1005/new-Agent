import hashlib
import re


def sha256(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


class JobFingerprint:
    def create(self, job) -> str:
        if job.source_job_id:
            material = f"{job.source}|{job.source_job_id}"
        else:
            material = "|".join((job.company_name or "", job.normalized_title, job.city or "", job.salary_text or ""))
        return sha256(re.sub(r"\s+", "", material).casefold())

    def content_hash(self, job) -> str:
        return sha256("|".join((job.job_title, job.salary_text or "", job.description or "", job.requirements or "")))
