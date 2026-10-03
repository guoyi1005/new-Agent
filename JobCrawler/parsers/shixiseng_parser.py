from datetime import date, datetime, timezone, timedelta
import re
from urllib.parse import urljoin, urlsplit

from bs4 import BeautifulSoup

from models.job import RawJobData
from parsers.base_parser import BaseParser
from sources.base import JobStub


# Selectors live here so source markup changes stay isolated from HTTP behavior.
DETAIL_LINK_SELECTOR = 'a[href*="/intern/inn_"]'
TITLE_SELECTOR = "title"
SALARY_SELECTOR = ".job_money"
LOCATION_SELECTOR = ".job_position"
EDUCATION_SELECTOR = ".job_academic"
EXPERIENCE_SELECTORS = (".job_week", ".job_time")
DESCRIPTION_SELECTOR = ".job_detail"
COMPANY_SELECTOR = ".com_position"
PUBLISH_TIME_SELECTORS = (".job_publish_time", ".job_time_info", "time")
BLOCKED_TEXT = ("访问验证", "滑动验证", "请先登录", "verify you are human")
PRIVATE_USE = re.compile("[\ue000-\uf8ff]")


def clean(value: str | None) -> str | None:
    if not value:
        return None
    text = BeautifulSoup(value, "lxml").get_text(" ", strip=True)
    text = PRIVATE_USE.sub(" ", text)
    text = re.sub(r"\s+", " ", text).strip(" ·|\t\r\n")
    return text or None


class ShixisengParser(BaseParser):
    def parse_list(self, html: str, base_url: str) -> list[JobStub]:
        soup = BeautifulSoup(html, "lxml")
        if any(marker in soup.get_text(" ", strip=True).lower() for marker in BLOCKED_TEXT):
            raise ValueError("verification/login page detected")
        result: list[JobStub] = []
        seen: set[str] = set()
        for anchor in soup.select(DETAIL_LINK_SELECTOR):
            url = urljoin(base_url, anchor.get("href", "").split("?", 1)[0])
            match = re.search(r"/intern/(inn_[a-z0-9]+)", url)
            if not match or match.group(1) in seen:
                continue
            seen.add(match.group(1))
            title = clean(anchor.get_text(" ", strip=True))
            title = re.sub(r"[\ue000-\uf8ff]", "", title or "").strip()
            ancestor = anchor
            card_text = ""
            for _ in range(5):
                if ancestor.parent is None:
                    break
                ancestor = ancestor.parent
                card_text = ancestor.get_text(" ", strip=True)
                if len(card_text) > 30:
                    break
            result.append(JobStub(match.group(1), url, title or None, city=self._city(card_text), salary_text=self._salary(card_text)))
        return result

    def parse_detail(self, html: str, stub: JobStub) -> RawJobData:
        soup = BeautifulSoup(html, "lxml")
        page_text = soup.get_text(" ", strip=True)
        if any(marker in page_text.lower() for marker in BLOCKED_TEXT):
            raise ValueError("verification/login page detected")
        title = clean(soup.select_one(TITLE_SELECTOR).get_text(" ", strip=True) if soup.select_one(TITLE_SELECTOR) else None)
        title = re.sub(r"\s*[-|｜].*$", "", title or "") or stub.job_title or ""
        title = re.sub(r"(?:实习生招聘|实习招聘|实习僧)$", "", title).strip()
        salary = self._first(soup, (SALARY_SELECTOR,)) or stub.salary_text
        city_text = self._first(soup, (LOCATION_SELECTOR,)) or stub.city
        city, district = self._split_location(city_text)
        education = self._first(soup, (EDUCATION_SELECTOR,))
        experience_values = [self._first(soup, (selector,)) for selector in EXPERIENCE_SELECTORS]
        experience = " · ".join(value for value in experience_values if value)
        company_text = self._first(soup, (COMPANY_SELECTOR,))
        company = self._company_from_title(soup) or self._company_from_text(company_text)
        details = []
        requirement_parts = []
        for node in soup.select(DESCRIPTION_SELECTOR):
            value = clean(node.get_text("\n", strip=True))
            heading = node.find_previous(class_=re.compile(r"job_til"))
            heading_text = clean(heading.get_text(" ", strip=True)) if heading else ""
            if value and re.search(r"要求|任职|资格|投递", heading_text or ""):
                requirement_parts.append(value)
            elif value:
                details.append(value)
        description = "\n".join(details) or None
        requirements = "\n".join(requirement_parts) or description
        if description and len(description) < 30:
            description = None
        publish_match = re.search(r"20\d{2}[-/]\d{1,2}[-/]\d{1,2}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?", page_text)
        publish_time = publish_match.group(0).replace("/", "-") if publish_match else None
        deadline_match = re.search(r"截止日期\s*[:：]?\s*(20\d{2}[-/]\d{1,2}[-/]\d{1,2})", page_text)
        is_active = "当前职位已下线" not in page_text
        if deadline_match:
            try:
                from datetime import date
                deadline = datetime.strptime(deadline_match.group(1).replace("/", "-"), "%Y-%m-%d").date()
                is_active = is_active and deadline >= date.today()
            except ValueError:
                pass
        is_recent = False
        if publish_match:
            try:
                posted = datetime.strptime(publish_match.group(0).replace("/", "-")[:10], "%Y-%m-%d").date()
                is_recent = posted >= date.today() - timedelta(days=90)
            except ValueError:
                pass
        return RawJobData(
            source="shixiseng", source_job_id=stub.source_job_id, source_url=stub.source_url,
            job_title=title, company_name=company, city=city, district=district,
            salary_text=salary, education=education, experience=experience or None,
            job_type="internship", description=description,
            requirements=requirements, company_industry=None, company_size=None,
            publish_time=publish_time, is_active=is_active, is_recent=is_recent,
            crawl_time=datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        )

    @staticmethod
    def _first(soup: BeautifulSoup, selectors: tuple[str, ...]) -> str | None:
        for selector in selectors:
            node = soup.select_one(selector)
            value = clean(node.get_text(" ", strip=True)) if node else None
            if value:
                return value
        return None

    @staticmethod
    def _city(text: str) -> str | None:
        match = re.search(r"(北京|上海|天津|重庆|[一-鿿]{2,8}市)", text)
        return match.group(1) if match else None

    @staticmethod
    def _salary(text: str) -> str | None:
        match = re.search(r"\d+(?:\.\d+)?\s*[-~至]\s*\d+(?:\.\d+)?\s*(?:K|k|千|元/天|/天|万/月)?", text)
        return match.group(0) if match else None

    @staticmethod
    def _split_location(value: str | None) -> tuple[str | None, str | None]:
        if not value:
            return None, None
        parts = [part.strip() for part in re.split(r"[/·,，\s]+", value) if part.strip()]
        return (parts[0] if parts else value, parts[-1] if len(parts) > 1 else None)

    @staticmethod
    def _company_from_title(soup: BeautifulSoup) -> str | None:
        full_title = clean(soup.select_one(TITLE_SELECTOR).get_text(" ", strip=True)) if soup.select_one(TITLE_SELECTOR) else None
        if not full_title:
            return None
        # The public title is formatted as job title - company - recruitment label.
        parts = re.split(r"\s*[-|｜]\s*", full_title)
        if len(parts) < 2:
            return None
        company = parts[-2] if len(parts) >= 3 and "实习僧" in parts[-1] else parts[-1]
        return company.replace("实习生招聘", "").replace("实习招聘", "").strip() or None

    @staticmethod
    def _company_from_text(text: str | None) -> str | None:
        if not text:
            return None
        parts = [part.strip() for part in text.split() if part.strip()]
        suffixes = ("有限责任公司", "股份有限公司", "有限公司", "集团", "公司", "科技", "电子")
        return next((part for part in reversed(parts) if any(part.endswith(suffix) for suffix in suffixes)), None)
