package com.example.appbackend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExtractorTest {
    @Test void extractsCanonicalSkillsFromAliases() {
        var skills = new SkillExtractor().extract("要求 springboot、JS、大模型和MySQL经验");
        var names = skills.stream().map(SkillExtractor.Skill::name).toList();
        assertTrue(names.contains("Spring Boot"));
        assertTrue(names.contains("JavaScript"));
        assertTrue(names.contains("LLM"));
        assertTrue(names.contains("MySQL"));
    }
}
