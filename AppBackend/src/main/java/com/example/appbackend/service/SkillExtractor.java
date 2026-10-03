package com.example.appbackend.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SkillExtractor {
    private static final Map<String, Skill> DICTIONARY = dictionary();

    public List<Skill> extract(String text) {
        if (text == null || text.isBlank()) return List.of();
        String lower = text.toLowerCase(Locale.ROOT);
        return DICTIONARY.values().stream().distinct()
                .filter(skill -> skill.aliases.stream().anyMatch(alias -> contains(lower, alias.toLowerCase(Locale.ROOT))))
                .toList();
    }

    private static boolean contains(String text, String alias) {
        if (alias.matches("[a-z0-9+#.]+")) return text.matches("(?s).*?(?<![a-z0-9])" + java.util.regex.Pattern.quote(alias) + "(?![a-z0-9]).*");
        return text.contains(alias);
    }

    private static Map<String, Skill> dictionary() {
        Map<String, Skill> map = new LinkedHashMap<>();
        add(map, "Java", "Programming", "java"); add(map, "Python", "Programming", "python");
        add(map, "C++", "Programming", "c++"); add(map, "JavaScript", "Programming", "javascript", "js");
        add(map, "TypeScript", "Programming", "typescript", "ts");
        add(map, "Spring Boot", "Backend", "spring boot", "springboot", "spring-boot");
        add(map, "Spring", "Backend", "spring"); add(map, "Django", "Backend", "django");
        add(map, "FastAPI", "Backend", "fastapi"); add(map, "Flask", "Backend", "flask");
        add(map, "Node.js", "Backend", "node.js", "nodejs"); add(map, "Vue", "Frontend", "vue", "vue.js");
        add(map, "React", "Frontend", "react", "react.js"); add(map, "MySQL", "Database", "mysql");
        add(map, "PostgreSQL", "Database", "postgresql", "postgres"); add(map, "Redis", "Database", "redis");
        add(map, "MongoDB", "Database", "mongodb"); add(map, "PyTorch", "AI", "pytorch");
        add(map, "TensorFlow", "AI", "tensorflow"); add(map, "LLM", "AI", "llm", "large language model", "大模型");
        add(map, "RAG", "AI", "rag"); add(map, "LangChain", "AI", "langchain");
        add(map, "Docker", "Engineering", "docker"); add(map, "Kubernetes", "Engineering", "kubernetes", "k8s");
        add(map, "Linux", "Engineering", "linux"); add(map, "Git", "Engineering", "git");
        add(map, "Nginx", "Engineering", "nginx");
        return java.util.Collections.unmodifiableMap(map);
    }

    private static void add(Map<String, Skill> map, String name, String category, String... aliases) {
        List<String> names = new java.util.ArrayList<>(List.of(aliases)); names.add(name);
        map.put(name, new Skill(name, name.toLowerCase(Locale.ROOT), category, List.copyOf(names)));
    }

    public record Skill(String name, String normalized, String category, List<String> aliases) {}
}
