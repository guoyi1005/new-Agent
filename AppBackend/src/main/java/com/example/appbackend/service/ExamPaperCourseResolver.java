package com.example.appbackend.service;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseExam;
import com.example.appbackend.repository.CampusCourseExamRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端试卷列表的辅助查询：把试卷 ID 反查成它作为「课程考试」挂在哪些课程下。
 * 一门课对应一套卷，一套卷理论上也可以挂到多门课上，这里用顿号合并展示。
 */
@Service
public class ExamPaperCourseResolver {

    private final CampusCourseExamRepository courseExamRepository;
    private final CampusCourseRepository courseRepository;

    public ExamPaperCourseResolver(CampusCourseExamRepository courseExamRepository,
                                   CampusCourseRepository courseRepository) {
        this.courseExamRepository = courseExamRepository;
        this.courseRepository = courseRepository;
    }

    public Map<Long, String> courseNamesByPaperId(Collection<Long> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return Map.of();
        }
        List<CampusCourseExam> links = courseExamRepository.findByPaperIdIn(paperIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> namesByCourseId = courseRepository
                .findAllById(links.stream().map(CampusCourseExam::getCourseId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(CampusCourse::getId, CampusCourse::getName));
        Map<Long, String> result = new HashMap<>();
        for (CampusCourseExam link : links) {
            String courseName = namesByCourseId.get(link.getCourseId());
            if (courseName == null) {
                continue;
            }
            result.merge(link.getPaperId(), courseName, (left, right) -> left + "、" + right);
        }
        return result;
    }
}
