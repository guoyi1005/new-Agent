package com.example.appbackend.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTextMatcherTest {

    @Test
    void problemTagsMapToTopicSkillPlusUmbrellaSkill() {
        assertEquals(List.of("algo-array-hash", SkillTextMatcher.DATA_STRUCTURES),
                SkillTextMatcher.skillsForProblemTags(List.of("数组", "哈希表")));
        assertEquals(List.of("algo-recursion-dp", SkillTextMatcher.DATA_STRUCTURES),
                SkillTextMatcher.skillsForProblemTags(List.of("动态规划")));
    }

    @Test
    void unknownOrEmptyTagsProduceNoSkills() {
        assertTrue(SkillTextMatcher.skillsForProblemTags(List.of("未知标签")).isEmpty());
        assertTrue(SkillTextMatcher.skillsForProblemTags(List.of()).isEmpty());
        assertTrue(SkillTextMatcher.skillsForProblemTags(null).isEmpty());
    }

    @Test
    void pathTextMatchesKnownSkillsAndIsCapped() {
        List<String> codes = SkillTextMatcher.matchText("FastAPI 接口开发与 MySQL 数据库联调");
        assertTrue(codes.contains("fastapi"));
        assertTrue(codes.contains("mysql"));
        assertTrue(codes.size() <= 3);
    }

    @Test
    void blankPathTextMatchesNothing() {
        assertTrue(SkillTextMatcher.matchText("").isEmpty());
        assertTrue(SkillTextMatcher.matchText(null).isEmpty());
    }
}