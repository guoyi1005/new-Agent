import sys
import unittest
from pathlib import Path
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from parsers.shixiseng_parser import ShixisengParser
from processors.fingerprint import JobFingerprint
from processors.quality_score import score
from processors.salary_parser import SalaryParser
from processors.skill_extractor import SkillExtractor
from processors.title_normalizer import JobTitleNormalizer
from sources.base import JobStub


FIXTURES = Path(__file__).parent / "fixtures"


class OfflineCrawlerTests(unittest.TestCase):
    def test_public_list_fixture_extracts_detail_url_and_id(self):
        html = (FIXTURES / "shixiseng_list.html").read_text(encoding="utf-8")
        jobs = ShixisengParser().parse_list(html, "https://www.shixiseng.com")
        self.assertEqual(len(jobs), 1)
        self.assertEqual(jobs[0].source_job_id, "inn_fixture123")
        self.assertTrue(jobs[0].source_url.endswith("/intern/inn_fixture123"))

    def test_detail_fixture_extracts_fields(self):
        html = (FIXTURES / "shixiseng_detail.html").read_text(encoding="utf-8")
        raw = ShixisengParser().parse_detail(html, JobStub("inn_fixture123", "https://www.shixiseng.com/intern/inn_fixture123"))
        self.assertEqual(raw.job_title, "Python 开发实习生")
        self.assertEqual(raw.city, "上海市")
        self.assertEqual(raw.salary_text, "200-250元/天")
        self.assertEqual(raw.company_name, "示例公司")
        self.assertIn("FastAPI", raw.description)
        self.assertIn("Redis", raw.requirements)
        self.assertNotIn("Redis", raw.description)
        self.assertTrue(raw.is_active)
        self.assertTrue(raw.is_recent)

    def test_detail_fixture_marks_explicitly_offline_job_inactive(self):
        html = (FIXTURES / "shixiseng_detail.html").read_text(encoding="utf-8").replace("</body>", "<p>当前职位已下线</p></body>")
        raw = ShixisengParser().parse_detail(html, JobStub("inn_fixture123", "https://www.shixiseng.com/intern/inn_fixture123"))
        self.assertFalse(raw.is_active)

    def test_company_parser_uses_full_name_before_address(self):
        company = ShixisengParser._company_from_text("上海市/上海市/闵行区 圣戈班研发(上海)有限公司 文井路55号")
        self.assertEqual(company, "圣戈班研发(上海)有限公司")

    def test_salary_parser_does_not_invent_negotiable_salary(self):
        parser = SalaryParser()
        self.assertEqual(parser.parse("10-15K·14薪").salary_min, 10000)
        self.assertEqual(parser.parse("200-300元/天").salary_unit, "day")
        self.assertEqual(parser.parse("20-30万/年").salary_max, 300000)
        self.assertIsNone(parser.parse("面议").salary_min)

    def test_title_and_skill_normalization(self):
        self.assertEqual(JobTitleNormalizer().normalize("Python后端实习生"), "Python开发工程师")
        self.assertEqual(set(SkillExtractor().extract("熟悉 SpringBoot、mysql 和 FastAPI")), {"Spring Boot", "MySQL", "FastAPI"})

    def test_fingerprint_and_quality_score(self):
        job = SimpleNamespace(source="shixiseng", source_job_id="inn_fixture123", company_name="示例公司",
                              normalized_title="Python开发工程师", city="上海", salary_text="200-250元/天",
                              job_title="Python开发实习生", description="真实描述 " * 30,
                              requirements="真实要求", education="本科", experience="实习3个月",
                              publish_time="2026-08-01", source_url="https://www.shixiseng.com/intern/inn_fixture123")
        fp = JobFingerprint()
        self.assertEqual(fp.create(job), fp.create(job))
        self.assertEqual(len(fp.content_hash(job)), 64)
        self.assertGreaterEqual(score(job), 90)


if __name__ == "__main__":
    unittest.main()
