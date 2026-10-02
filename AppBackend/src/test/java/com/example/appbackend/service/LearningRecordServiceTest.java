package com.example.appbackend.service;

import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningRecordServiceTest {

    private LearningRecordRepository records;
    private LearningContentSkillRepository contentSkills;
    private LearningSkillRepository skills;
    private LearningRecordService service;

    @BeforeEach
    void setUp() {
        records = mock(LearningRecordRepository.class);
        contentSkills = mock(LearningContentSkillRepository.class);
        skills = mock(LearningSkillRepository.class);
        service = new LearningRecordService(records, contentSkills, skills, new ObjectMapper());
        when(records.existsByEventId(anyString())).thenReturn(false);
        when(records.findByUserIdAndSkillIdOrderByOccurredAtDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(records.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(skills.findByCode("algo-array-hash")).thenReturn(Optional.of(skill(11L, "algo-array-hash")));
        when(skills.findByCode("algo-recursion-dp")).thenReturn(Optional.of(skill(13L, "algo-recursion-dp")));
        when(skills.findByCode("data-structures")).thenReturn(Optional.of(skill(12L, "data-structures")));
    }

    @Test
    void easyProblemAdvancesEachMappedSkillByTen() {
        PythonProblem problem = new PythonProblem();
        problem.setId(1L);
        problem.setTitle("两数之和");
        problem.setDifficulty("easy");
        problem.setTags("[\"数组\"]");

        List<LearningRecord> written = service.recordProblemSolved(7L, problem);

        assertEquals(2, written.size());
        assertTrue(written.stream().allMatch(record -> record.getProgress() == 10));
        assertTrue(written.stream().allMatch(record -> LearningRecordService.SOURCE_PROBLEM.equals(record.getSourceType())));
        assertTrue(written.stream().allMatch(record -> LearningRecordService.ACTION_PROBLEM_SOLVED.equals(record.getActionType())));
    }

    @Test
    void hardProblemAdvancesByTwentyAndCapsAtHundred() {
        when(records.findByUserIdAndSkillIdOrderByOccurredAtDesc(anyLong(), anyLong()))
                .thenReturn(List.of(recordWithProgress(95)));
        PythonProblem problem = new PythonProblem();
        problem.setId(2L);
        problem.setTitle("困难的题");
        problem.setDifficulty("hard");
        problem.setTags("[\"动态规划\"]");

        List<LearningRecord> written = service.recordProblemSolved(7L, problem);

        assertEquals(2, written.size());
        assertTrue(written.stream().allMatch(record -> record.getProgress() == 100));
    }

    @Test
    void problemWithoutKnownTagsWritesNothing() {
        PythonProblem problem = new PythonProblem();
        problem.setId(3L);
        problem.setTitle("陌生题");
        problem.setDifficulty("easy");
        problem.setTags("[\"未知标签\"]");

        assertTrue(service.recordProblemSolved(7L, problem).isEmpty());
    }

    @Test
    void pathItemWithoutMatchedTextWritesNothing() {
        assertTrue(service.recordPathItemCompleted(7L, 99L, "随手记录").isEmpty());
    }

    @Test
    void currentSkillLevelsTakeHighestProgressPerSkill() {
        LearningRecord low = recordWithProgress(30);
        low.setSkillId(11L);
        LearningRecord high = recordWithProgress(70);
        high.setSkillId(11L);
        when(records.findByUserIdOrderByOccurredAtDesc(7L)).thenReturn(List.of(low, high));

        assertEquals(70, service.currentSkillLevels(7L).get(11L));
    }

    private LearningSkill skill(Long id, String code) {
        LearningSkill skill = new LearningSkill();
        skill.setId(id);
        skill.setCode(code);
        return skill;
    }

    private LearningRecord recordWithProgress(int progress) {
        LearningRecord record = new LearningRecord();
        record.setProgress(progress);
        return record;
    }
}