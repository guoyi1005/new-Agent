package com.example.appbackend.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class JobTitleNormalizer {
    private final List<Rule> rules = List.of(
            rule("大模型工程师", "大模型|LLM|large language model"),
            rule("AI应用开发工程师", "AI应用|人工智能应用|大模型应用"),
            rule("机器学习工程师", "机器学习|machine learning"),
            rule("算法工程师", "算法工程师|算法开发"),
            rule("Python开发工程师", "Python.*(开发|后端|工程师)|(开发|后端|工程师).*Python|^Python$"),
            rule("Java后端开发", "Java.*(后端|开发|工程师)|后端.*Java"),
            rule("前端开发工程师", "前端.*(开发|工程师)|Web前端"),
            rule("全栈开发工程师", "全栈|full.?stack"),
            rule("测试工程师", "测试.*(工程师|开发)|QA工程师"),
            rule("运维工程师", "运维|SRE|DevOps"),
            rule("数据分析师", "数据分析|商业分析师|BI分析师"),
            rule("数据工程师", "数据工程师|大数据开发"),
            rule("产品经理", "产品经理|产品专员"),
            rule("UI设计师", "UI设计|用户界面设计"),
            rule("UX设计师", "UX设计|用户体验设计")
    );

    public String normalize(String title) {
        if (title == null || title.isBlank()) return "";
        String normalized = title.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[（()）]", "")
                .replaceAll("实习生?|校招|应届生|高级|资深|初级|中级|工程师", "")
                .replaceAll("[\\s_·/\\-]", "");
        for (Rule rule : rules) if (rule.pattern.matcher(normalized).find()) return rule.name;
        return title.trim().replaceAll("[（()）]", "").replaceAll("\\s+", " ");
    }

    private static Rule rule(String name, String regex) {
        return new Rule(name, Pattern.compile(regex.replace(" ", ""), Pattern.CASE_INSENSITIVE));
    }

    private record Rule(String name, Pattern pattern) {}
}
