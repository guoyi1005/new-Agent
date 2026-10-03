package com.example.appbackend.service;

import com.example.appbackend.entity.CampusCourseEnrollment;
import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseEnrollmentRepository;
import com.example.appbackend.repository.CampusCourseProgressRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.repository.PythonProblemRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningPracticeSummaryServiceTest {

    @Test
    void verifiedSummaryDoesNotTreatSeededHistoryAsPersonalEvidence() {
        var enrollments = mock(CampusCourseEnrollmentRepository.class);
        var courses = mock(CampusCourseRepository.class);
        var chapters = mock(CampusCourseChapterRepository.class);
        var progress = mock(CampusCourseProgressRepository.class);
        var contentSkills = mock(LearningContentSkillRepository.class);
        var skills = mock(LearningSkillRepository.class);
        var records = mock(LearningRecordRepository.class);
        var problems = mock(PythonProblemRepository.class);
        var paths = mock(LearningPathService.class);
        var service = new LearningPracticeSummaryService(enrollments, courses, chapters, progress,
                contentSkills, skills, records, problems, paths);

        LearningSkill python = new LearningSkill();
        python.setId(3L);
        python.setCode("python");
        python.setName("Python");
        when(skills.findAll()).thenReturn(List.of(python));

        LearningRecord demoCourse = record("demo-course-1-7-3-0", "COURSE", 7L, 3L, 76);
        LearningRecord demoProblem = record("demo-problem-1-101-0-0", "PROBLEM", 101L, 3L, 72);
        LearningRecord realProblem = record("problem-102-user-1-skill-3-solved", "PROBLEM", 102L, 3L, 86);
        when(records.findByUserIdOrderByOccurredAtDesc(1L))
                .thenReturn(List.of(realProblem, demoCourse, demoProblem));

        CampusCourseEnrollment seededEnrollment = new CampusCourseEnrollment();
        seededEnrollment.setCourseId(7L);
        when(enrollments.findByUserIdOrderByEnrolledTimeDesc(1L)).thenReturn(List.of(seededEnrollment));
        when(problems.findByEnabledTrueOrderByNumberAsc()).thenReturn(List.of());

        var summary = service.summary(1L, true);

        assertEquals(0, summary.getCourses().size());
        assertEquals(1, summary.getProblems().getSolvedCount());
        assertEquals(1, summary.getSkills().size());
        assertEquals(1, summary.getSkills().get(0).getEvidenceCount());
        assertNull(summary.getSkills().get(0).getLevel());
    }

    private LearningRecord record(String eventId, String sourceType, Long sourceId, Long skillId, int level) {
        LearningRecord record = new LearningRecord();
        record.setEventId(eventId);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setSkillId(skillId);
        record.setProgress(level);
        return record;
    }
}
