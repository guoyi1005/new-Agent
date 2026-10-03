"""Manually collect a bounded sample of public internship pages and write JSON."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import json
import logging
from pathlib import Path
import sys

from config import KEYWORDS, MAX_PER_KEYWORD, OUTPUT
from processors.record import build_record
from sources.base import SourceStopped
from sources.shixiseng import ShixisengSource


LOG_PATH = OUTPUT / "crawl.log"
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s",
                    handlers=[logging.FileHandler(LOG_PATH, encoding="utf-8"), logging.StreamHandler(sys.stdout)])
log = logging.getLogger("job-crawler")


def parse_args():
    parser = argparse.ArgumentParser(description="Low-rate public job-page collector (HTTP only)")
    parser.add_argument("--keywords", default=",".join(KEYWORDS), help="Comma-separated public list-page keywords")
    parser.add_argument("--limit", type=int, default=MAX_PER_KEYWORD, help="Maximum details per keyword (hard capped at 30)")
    parser.add_argument("--min-quality", type=int, default=50)
    parser.add_argument("--output", default=str(OUTPUT / "jobs.json"), help="JSON output path")
    return parser.parse_args()


def main():
    args = parse_args()
    keywords = [value.strip() for value in args.keywords.split(",") if value.strip()]
    per_keyword = max(1, min(30, args.limit))
    source = ShixisengSource()
    jobs, seen = [], set()
    stats = Counter({key: 0 for key in (
        "list_pages", "discovered_count", "detail_requested_count", "detail_success_count",
        "success_count", "duplicate_count", "failed_count", "blocked_count",
        "low_quality_count", "salary_parsed_count", "skill_extracted_count")})
    keyword_runs = []
    started = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
    try:
        for keyword in keywords:
            run = {"source": source.source_name, "keyword": keyword,
                   "start_time": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
                   "finish_time": None, "list_pages": 0, "discovered_count": 0,
                   "detail_requested_count": 0, "detail_success_count": 0,
                   "success_count": 0, "duplicate_count": 0, "failed_count": 0,
                   "blocked_count": 0, "error": None}
            keyword_runs.append(run)
            if source.status in ("BLOCKED", "UNAVAILABLE"):
                log.warning("source=%s stopped with status=%s; remaining keywords skipped", source.source_name, source.status)
                run["error"] = f"source stopped: {source.status}"
                run["finish_time"] = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
                break
            discovered = []
            try:
                discovered = source.discover_jobs(keyword, per_keyword)
                stats["list_pages"] += 1
                stats["discovered_count"] += len(discovered)
                run["list_pages"] += 1
                run["discovered_count"] += len(discovered)
                log.info("source=%s keyword=%s discovered=%s status=%s", source.source_name, keyword, len(discovered), source.status)
            except (SourceStopped, ValueError) as exc:
                stats["failed_count"] += 1
                stats["blocked_count"] += int(getattr(exc, "status", "") == "BLOCKED")
                run["failed_count"] += 1
                run["blocked_count"] += int(getattr(exc, "status", "") == "BLOCKED")
                run["error"] = str(exc)
                log.warning("source=%s keyword=%s status=%s error=%s", source.source_name, keyword, getattr(exc, "status", "UNAVAILABLE"), exc)
                run["finish_time"] = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
                continue

            for stub in discovered:
                if stub.source_job_id in seen:
                    stats["duplicate_count"] += 1
                    run["duplicate_count"] += 1
                    continue
                seen.add(stub.source_job_id)
                stats["detail_requested_count"] += 1
                run["detail_requested_count"] += 1
                try:
                    raw = source.fetch_detail(stub)
                    stats["detail_success_count"] += 1
                    run["detail_success_count"] += 1
                    record = build_record(raw)
                    if record["quality_score"] < args.min_quality:
                        stats["low_quality_count"] += 1
                        continue
                    jobs.append(record)
                    stats["success_count"] += 1
                    run["success_count"] += 1
                    stats["salary_parsed_count"] += int(record["salary_min"] is not None)
                    stats["skill_extracted_count"] += int(bool(record["skills"]))
                except SourceStopped as exc:
                    stats["failed_count"] += 1
                    stats["blocked_count"] += int(exc.status == "BLOCKED")
                    run["failed_count"] += 1
                    run["blocked_count"] += int(exc.status == "BLOCKED")
                    run["error"] = str(exc)
                    log.warning("source=%s job=%s status=%s error=%s", source.source_name, stub.source_job_id, exc.status, exc)
                    if exc.status == "BLOCKED":
                        break
                except Exception as exc:  # Keep one malformed page from aborting remaining results.
                    stats["failed_count"] += 1
                    run["failed_count"] += 1
                    run["error"] = str(exc)
                    log.warning("source=%s job=%s status=PARSE_ERROR error=%s", source.source_name, stub.source_job_id, exc)
            run["finish_time"] = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
    finally:
        source.close()

    deduplicated = {}
    for job in jobs:
        deduplicated.setdefault(job["fingerprint"], job)
    jobs = list(deduplicated.values())
    finished = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
    quality = Counter(str((job["quality_score"] // 10) * 10) for job in jobs)
    stats["active_count"] = sum(bool(job.get("is_active")) for job in jobs)
    stats["recent_count"] = sum(bool(job.get("is_recent")) for job in jobs)
    stats["offline_count"] = len(jobs) - stats["active_count"]
    stats["stale_count"] = len(jobs) - stats["recent_count"]
    stats["importable_count"] = sum(job.get("quality_score", 0) >= 50 and bool(job.get("is_active")) and bool(job.get("is_recent")) for job in jobs)
    output = {
        "schema_version": 1,
        "source_summary": {"shixiseng": source.status},
        "started_at": started,
        "finished_at": finished,
        "keyword_runs": keyword_runs,
        "stats": {**dict(stats), "deduplicated_count": len(jobs), "quality_score_buckets": dict(quality)},
        "jobs": jobs,
    }
    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")
    log.info("finished source=%s discovered=%s details=%s accepted=%s unique=%s output=%s",
             source.status, stats["discovered_count"], stats["detail_success_count"], stats["success_count"], len(jobs), out_path)
    print(json.dumps({"output": str(out_path), "source_status": source.status, "stats": output["stats"]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
