import re


RULES = (
    ("大模型应用开发", r"大模型.*应用|llm.*应用"),
    ("AI应用开发工程师", r"ai.*应用|人工智能.*应用"),
    ("大模型工程师", r"大模型|llm"),
    ("Java后端开发", r"java.*(后端|开发|工程)|后端.*java"),
    ("Python开发工程师", r"python.*(开发|后端|工程)|(?<![a-z])python(?![a-z])"),
    ("前端开发工程师", r"前端.*(开发|工程)|web前端"),
    ("数据分析师", r"数据分析|商业分析|bi分析"),
    ("产品经理", r"产品经理|产品专员"),
)


class JobTitleNormalizer:
    def normalize(self, title: str) -> str:
        original = re.sub(r"\s+", " ", (title or "").strip())
        key = re.sub(r"[\s()（）【】\[\]·_/\-]", "", original).lower()
        key = re.sub(r"实习生?|校招|应届生|高级|资深|初级", "", key)
        for result, pattern in RULES:
            if re.search(pattern, key, re.IGNORECASE):
                return result
        return original
