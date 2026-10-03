package com.example.appbackend.config;

import com.example.appbackend.dto.ExamPaperDTO;
import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseExam;
import com.example.appbackend.entity.ExamPaper;
import com.example.appbackend.entity.ExamPaperQuestion;
import com.example.appbackend.entity.ExamQuestion;
import com.example.appbackend.repository.CampusCourseExamRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.ExamPaperQuestionRepository;
import com.example.appbackend.repository.ExamPaperRepository;
import com.example.appbackend.repository.ExamQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 课程配套测验：给「课程与专项」里的每门课程生成一套测验卷，
 * 学生可以在「我的试卷」里直接作答。
 *
 * 题目来自 resources/seed/course-exam-papers.json，每门课 5 题、每题 20 分、满分 100 分。
 * 幂等：试卷按标题查重，题目按「来源 + 稳定编号」查重，重复启动不会产生重复数据。
 */
@Component
@Order(120)
public class CourseExamPaperInitializer implements ApplicationRunner {

    private static final String SOURCE_AGENT = "course-exam-seed";
    /**
     * 早期演示脚手架给课程 1-5 统一挂的通用期末卷。
     * 现在每门课都有自己的配套测验，把它从这些课程上摘掉，避免「课程考试」出现两道泛化的卷子。
     */
    private static final String LEGACY_DEMO_PAPER_TITLE = "岗位探索演示期末考试";
    private static final String SEED_PATH = "seed/course-exam-papers.json";
    private static final int SCORE_PER_QUESTION = 20;

    private final ExamQuestionRepository questionRepository;
    private final ExamPaperRepository paperRepository;
    private final ExamPaperQuestionRepository paperQuestionRepository;
    private final CampusCourseRepository courseRepository;
    private final CampusCourseExamRepository courseExamRepository;
    private final ObjectMapper objectMapper;

    public CourseExamPaperInitializer(
            ExamQuestionRepository questionRepository,
            ExamPaperRepository paperRepository,
            ExamPaperQuestionRepository paperQuestionRepository,
            CampusCourseRepository courseRepository,
            CampusCourseExamRepository courseExamRepository,
            ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.courseRepository = courseRepository;
        this.courseExamRepository = courseExamRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        List<CoursePaperSeed> seeds = readSeeds();
        for (CoursePaperSeed seed : seeds) {
            if (seed.questions() == null || seed.questions().isEmpty()) continue;
            ExamPaper paper = paperRepository.findFirstByTitleAndStatus(seed.paperTitle(), 1)
                    .orElseGet(this::createPaper);
            configurePaper(paper, seed);
            paper = paperRepository.save(paper);

            int order = 1;
            for (CourseQuestionSeed item : seed.questions()) {
                String sourceQuestionId = sourceQuestionId(seed.course(), order);
                ExamQuestion question = questionRepository
                        .findBySourceAgentAndSourceQuestionId(SOURCE_AGENT, sourceQuestionId)
                        .orElseGet(ExamQuestion::new);
                configureQuestion(question, seed, item, sourceQuestionId);
                question = questionRepository.save(question);

                ExamPaperQuestion link = paperQuestionRepository
                        .findFirstByPaperIdAndQuestionId(paper.getId(), question.getId())
                        .orElseGet(ExamPaperQuestion::new);
                configureLink(link, paper, question, order);
                paperQuestionRepository.save(link);
                order++;
            }

            // 把试卷挂到同名校课程的「课程考试」板块
            linkPaperToCourse(seed.course(), paper.getId());
        }
    }

    /**
     * 课程考试入口由 campus_course_exam 关联表驱动。
     * 已存在关联时只更新章节范围，不重复插入；排序放在已有考试之后。
     */
    private void linkPaperToCourse(String courseName, Long paperId) {
        CampusCourse course = courseRepository.findAllByOrderBySortOrderAscUpdateTimeDesc().stream()
                .filter(item -> courseName.equals(item.getName()))
                .findFirst()
                .orElse(null);
        if (course == null) return;
        CampusCourseExam link = courseExamRepository.findByCourseIdAndPaperId(course.getId(), paperId)
                .orElseGet(CampusCourseExam::new);
        if (link.getId() == null) {
            int nextOrder = courseExamRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId()).stream()
                    .map(CampusCourseExam::getSortOrder)
                    .filter(java.util.Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            link.setSortOrder(nextOrder);
        }
        link.setCourseId(course.getId());
        link.setPaperId(paperId);
        link.setChapterScope("全部章节");
        courseExamRepository.save(link);

        // 该课程已有自己的配套测验，移除遗留的通用演示考试
        paperRepository.findFirstByTitleAndStatus(LEGACY_DEMO_PAPER_TITLE, 1)
                .filter(demo -> !demo.getId().equals(paperId))
                .flatMap(demo -> courseExamRepository.findByCourseIdAndPaperId(course.getId(), demo.getId()))
                .ifPresent(courseExamRepository::delete);
    }

    private List<CoursePaperSeed> readSeeds() throws Exception {
        ClassPathResource resource = new ClassPathResource(SEED_PATH);
        if (!resource.exists()) return List.of();
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<List<CoursePaperSeed>>() { });
        }
    }

    private ExamPaper createPaper() {
        ExamPaper paper = new ExamPaper();
        paper.setCreatedBy(1L);
        paper.setStatus(1);
        paper.setPublished(true);
        paper.setPublishTime(LocalDateTime.now());
        paper.setPageSize(ExamPaperDTO.PageSize.A4);
        paper.setOrientation(ExamPaperDTO.Orientation.PORTRAIT);
        paper.setColumnsCount(1);
        paper.setSelectionMode(ExamPaperDTO.SelectionMode.MANUAL);
        return paper;
    }

    private void configurePaper(ExamPaper paper, CoursePaperSeed seed) {
        paper.setTitle(seed.paperTitle());
        paper.setSubtitle("《" + seed.course() + "》配套测验，共 " + seed.questions().size()
                + " 题，每题 " + SCORE_PER_QUESTION + " 分，满分 100 分");
        paper.setDurationMinutes(30);
        paper.setPrecautions("请独立完成答题，提交后可查看成绩与解析。");
        paper.setQuestionCount(seed.questions().size());
        paper.setTotalScore(BigDecimal.valueOf((long) seed.questions().size() * SCORE_PER_QUESTION));
        paper.setPublished(true);
        if (paper.getPublishTime() == null) paper.setPublishTime(LocalDateTime.now());
    }

    private void configureQuestion(ExamQuestion question, CoursePaperSeed seed,
                                   CourseQuestionSeed item, String sourceQuestionId) {
        question.setSourceQuestionId(sourceQuestionId);
        question.setSourceTitle(seed.course());
        question.setSourceAgent(SOURCE_AGENT);
        question.setSourceScene("seed");
        question.setVisibility(ExamQuestion.VISIBILITY_PUBLIC);
        question.setStatus(1);
        question.setScore(BigDecimal.valueOf(SCORE_PER_QUESTION));
        question.setDifficulty(StringUtils.hasText(item.difficulty()) ? item.difficulty() : "easy");
        question.setTagsJson(json(List.of(seed.course(), "课程测验")));
        question.setKnowledgePointsJson(json(seed.knowledgePoints() == null ? List.of() : seed.knowledgePoints()));
        question.setScoringJson(json(Map.of("mode", "exact", "rubrics", List.of())));
        question.setStem(item.stem());
        question.setAnalysis(item.analysis());
        // 数据库 raw_question_json 为 NOT NULL，这里存一份可追溯的题目快照
        question.setRawQuestionJson(json(rawSnapshot(seed, item)));

        if ("true_false".equals(item.type())) {
            question.setType("true_false");
            question.setBodyJson(json(Map.of("statement", item.stem())));
            question.setAnswerJson(json(Map.of("correct", Boolean.TRUE.equals(item.correct()))));
        } else {
            question.setType("single_choice");
            List<Map<String, String>> options = new ArrayList<>();
            for (OptionSeed option : item.options() == null ? List.<OptionSeed>of() : item.options()) {
                options.add(Map.of("key", option.key(), "text", option.text()));
            }
            question.setBodyJson(json(Map.of("options", options, "shuffleOptions", false)));
            question.setAnswerJson(json(Map.of("correctOption", item.correctOption())));
        }
    }

    private void configureLink(ExamPaperQuestion link, ExamPaper paper, ExamQuestion question, int order) {
        link.setPaperId(paper.getId());
        link.setQuestionId(question.getId());
        link.setSortOrder(order);
        link.setSectionOrder(sectionOrder(question.getType()));
        link.setScore(question.getScore());
        link.setType(question.getType());
        link.setStem(question.getStem());
        link.setBodyJson(question.getBodyJson());
        link.setAnswerJson(question.getAnswerJson());
        link.setAnalysis(question.getAnalysis());
        link.setScoringJson(question.getScoringJson());
    }

    private int sectionOrder(String type) {
        if (type == null) return 1;
        return switch (type) {
            case "single_choice" -> 1;
            case "multiple_choice" -> 2;
            case "true_false" -> 3;
            case "fill_blank" -> 4;
            default -> 5;
        };
    }

    private String sourceQuestionId(String course, int order) {
        return SOURCE_AGENT + "-" + course + "-" + order;
    }

    /** 原始题目快照：保留生成来源与题干结构，便于后续追溯与再组卷。 */
    private Map<String, Object> rawSnapshot(CoursePaperSeed seed, CourseQuestionSeed item) {
        Map<String, Object> raw = new java.util.LinkedHashMap<>();
        raw.put("source", SOURCE_AGENT);
        raw.put("course", seed.course());
        raw.put("type", "true_false".equals(item.type()) ? "true_false" : "single_choice");
        raw.put("stem", item.stem());
        if (item.options() != null) raw.put("options", item.options());
        if (item.correctOption() != null) raw.put("correctOption", item.correctOption());
        if (item.correct() != null) raw.put("correct", item.correct());
        raw.put("analysis", item.analysis());
        return raw;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private record CoursePaperSeed(String course, String paperTitle,
                                   List<String> knowledgePoints,
                                   List<CourseQuestionSeed> questions) { }

    private record CourseQuestionSeed(String type, String difficulty, String stem,
                                      List<OptionSeed> options, String correctOption,
                                      Boolean correct, String analysis) { }

    private record OptionSeed(String key, String text) { }
}
