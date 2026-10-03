
def score(job) -> int:
    return min(100,
        (15 if job.job_title else 0) + (15 if job.company_name else 0) +
        (10 if job.city else 0) + (15 if job.salary_text else 0) +
        (20 if (job.description or job.requirements) and len(job.description or job.requirements) >= 80 else 0) +
        (5 if job.education else 0) + (5 if job.experience else 0) +
        (5 if job.publish_time else 0) + (10 if job.source_url else 0))
