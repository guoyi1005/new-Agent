"""Explicitly import a reviewed jobs.json into the existing Spring Boot job table."""
import argparse
import json
import re

import httpx

from config import APP_API_BASE, APP_API_TOKEN, OUTPUT


def to_api_request(job):
    """Keep the reviewed JSON readable while using the backend's Java field names."""
    return {re.sub(r"_([a-z])", lambda match: match.group(1).upper(), key): value
            for key, value in job.items()}


def main():
    parser = argparse.ArgumentParser(description="Import quality-approved records through the admin API")
    parser.add_argument("--file", default=str(OUTPUT / "jobs.json"))
    args = parser.parse_args()
    if not APP_API_TOKEN:
        raise SystemExit("APP_API_TOKEN is required; import is never anonymous")
    document = json.loads(open(args.file, encoding="utf-8").read())
    jobs = [to_api_request(job) for job in document.get("jobs", [])
            if job.get("quality_score", 0) >= 50 and job.get("is_active", False)
            and job.get("is_recent", False) and job.get("company_name") and job.get("source_url")]
    if not jobs:
        raise SystemExit("No jobs with quality_score >= 50; nothing imported")
    with httpx.Client(timeout=httpx.Timeout(30, connect=10)) as client:
        response = client.post(f"{APP_API_BASE}/api/admin/jobs/import-batch",
                               headers={"Authorization": f"Bearer {APP_API_TOKEN}"}, json=jobs)
    if response.is_error:
        raise SystemExit(f"Import failed ({response.status_code}): {response.text[:1000]}")
    print(response.text)


if __name__ == "__main__":
    main()
