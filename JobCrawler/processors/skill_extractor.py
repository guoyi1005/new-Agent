import re


TERMS = {
    "Java": ["java"], "Python": ["python"], "C++": ["c++"],
    "JavaScript": ["javascript"], "TypeScript": ["typescript"],
    "Spring Boot": ["spring boot", "springboot", "spring-boot"], "Spring": ["spring"],
    "MyBatis": ["mybatis"], "FastAPI": ["fastapi"], "Django": ["django"], "Flask": ["flask"],
    "Vue": ["vue", "vue.js"], "React": ["react", "react.js"], "MySQL": ["mysql"],
    "PostgreSQL": ["postgresql", "postgres"], "Redis": ["redis"], "MongoDB": ["mongodb"],
    "Linux": ["linux"], "Docker": ["docker"], "Kubernetes": ["kubernetes", "k8s"],
    "Git": ["git"], "Nginx": ["nginx"], "PyTorch": ["pytorch"], "TensorFlow": ["tensorflow"],
    "LLM": ["llm", "large language model", "大模型"], "RAG": ["rag"], "LangChain": ["langchain"],
    "Transformers": ["transformers"],
}


class SkillExtractor:
    def extract(self, description: str | None, requirements: str | None = None) -> list[str]:
        text = f"{description or ''} {requirements or ''}".lower()
        found = []
        for name, aliases in TERMS.items():
            if any(re.search(r"(?<![a-z0-9])" + re.escape(alias) + r"(?![a-z0-9])", text) if re.fullmatch(r"[a-z0-9+#.\- ]+", alias) else alias.lower() in text for alias in aliases):
                found.append(name)
        return found
