from types import SimpleNamespace

from processors.fingerprint import JobFingerprint
from processors.quality_score import score
from processors.salary_parser import SalaryParser
from processors.skill_extractor import SkillExtractor
from processors.title_normalizer import JobTitleNormalizer


def build_record(raw):
    salary = SalaryParser().parse(raw.salary_text)
    record = {
        **raw.model_dump(mode="json"),
        "normalized_title": JobTitleNormalizer().normalize(raw.job_title),
        "salary_min": float(salary.salary_min) if salary.salary_min is not None else None,
        "salary_max": float(salary.salary_max) if salary.salary_max is not None else None,
        "salary_unit": salary.salary_unit,
        "salary_months": salary.salary_months,
        "skills": SkillExtractor().extract(raw.description, raw.requirements),
    }
    job = SimpleNamespace(**record)
    fingerprints = JobFingerprint()
    record["fingerprint"] = fingerprints.create(job)
    record["content_hash"] = fingerprints.content_hash(job)
    record["quality_score"] = score(job)
    return record
