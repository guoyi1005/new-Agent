from pathlib import Path
import os

from dotenv import load_dotenv

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "output"
OUTPUT.mkdir(exist_ok=True)
load_dotenv(ROOT / ".env")

USER_AGENT = os.getenv("CRAWLER_USER_AGENT", "CampusJobResearch/1.0")
MIN_DELAY = max(1.0, float(os.getenv("CRAWLER_MIN_DELAY_SECONDS", "1.5")))
MAX_DELAY = max(MIN_DELAY, min(3.0, float(os.getenv("CRAWLER_MAX_DELAY_SECONDS", "3"))))
MAX_PER_KEYWORD = max(1, min(30, int(os.getenv("CRAWLER_MAX_PER_KEYWORD", "20"))))
APP_API_BASE = os.getenv("APP_API_BASE", "http://127.0.0.1:8080").rstrip("/")
APP_API_TOKEN = os.getenv("APP_API_TOKEN", "")

# The first release only uses one public, HTML-rendered source. These are normal
# search terms on the public list UI, not private endpoints.
KEYWORDS = ("Java", "Python", "AI应用", "大模型", "前端", "数据分析", "产品经理")
