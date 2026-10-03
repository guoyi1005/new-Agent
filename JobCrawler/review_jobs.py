"""Recheck importable listings against their public detail pages before import."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re

from processors.record import build_record
from sources.base import JobStub, SourceStopped
from sources.shixiseng import ShixisengSource


DETAIL_URL = re.compile(r"https://www\.shixiseng\.com/intern/inn_[a-z0-9]+$")


def main():
    parser = argparse.ArgumentParser(description="Recheck public job details before database import")
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--limit", type=int, default=200)
    args = parser.parse_args()

    document = json.loads(Path(args.input).read_text(encoding="utf-8"))
    candidates = [job for job in document.get("jobs", [])
                  if job.get("quality_score", 0) >= 50 and job.get("is_active") and job.get("is_recent")]
    source = ShixisengSource()
    reviewed = []
    failed = []
    try:
        for job in candidates[:max(1, min(args.limit, 200))]:
            url = job.get("source_url", "")
            if not DETAIL_URL.fullmatch(url):
                failed.append({"source_url": url, "reason": "unsupported detail URL"})
                continue
            stub = JobStub(job["source_job_id"], url, job.get("job_title"),
                           job.get("company_name"), job.get("city"), job.get("salary_text"))
            try:
                record = build_record(source.fetch_detail(stub))
            except (SourceStopped, ValueError) as exc:
                failed.append({"source_url": url, "reason": str(exc)})
                if isinstance(exc, SourceStopped) and exc.status == "BLOCKED":
                    break
                continue
            if (record["is_active"] and record["is_recent"] and record["quality_score"] >= 50
                    and record["company_name"] and record["job_title"]):
                reviewed.append(record)
            else:
                failed.append({"source_url": url, "reason": "no longer importable"})
    finally:
        source.close()

    result = {
        "schema_version": 1,
        "source_summary": {"shixiseng": source.status},
        "reviewed_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "review_stats": {"candidates": len(candidates), "accepted": len(reviewed), "rejected": len(failed)},
        "rejected": failed,
        "jobs": reviewed,
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(output), **result["review_stats"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
