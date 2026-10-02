package com.example.appbackend.config;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.entity.ExternalCourse;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.ExternalCourseRepository;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.util.SkillTextMatcher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一学习体系的基础数据：技能字典、岗位技能要求、课程与章节的技能关联。
 * 幂等初始化，重复启动不会产生重复数据，也不会覆盖管理员已经维护的课程内容。
 */
@Component
@Order(210)
public class LearningTaxonomyInitializer implements ApplicationRunner {

    private static final String SOURCE_COURSE = "COURSE";
    private static final String SOURCE_COURSE_CHAPTER = "COURSE_CHAPTER";
    private static final String SOURCE_EXTERNAL_COURSE = "EXTERNAL_COURSE";

    private final LearningSkillRepository skillRepository;
    private final JobSkillRequirementRepository jobRequirementRepository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final CampusCourseRepository courseRepository;
    private final CampusCourseChapterRepository chapterRepository;
    private final ExternalCourseRepository externalCourseRepository;

    public LearningTaxonomyInitializer(
            LearningSkillRepository skillRepository,
            JobSkillRequirementRepository jobRequirementRepository,
            LearningContentSkillRepository contentSkillRepository,
            CampusCourseRepository courseRepository,
            CampusCourseChapterRepository chapterRepository,
            ExternalCourseRepository externalCourseRepository
    ) {
        this.skillRepository = skillRepository;
        this.jobRequirementRepository = jobRequirementRepository;
        this.contentSkillRepository = contentSkillRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.externalCourseRepository = externalCourseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, LearningSkill> skills = ensureSkills();
        ensureJobRequirements(skills);
        ensureStarterCourses();
        ensureCourseLinks(skills);
        ensureChapterLinks(skills);
        ensureExternalCourses();
        ensureExternalCourseLinks(skills);
    }

    private Map<String, LearningSkill> ensureSkills() {
        List<SkillSeed> seeds = List.of(
                new SkillSeed("software-testing", "软件测试基础", "测试", "理解软件测试流程、测试类型与缺陷生命周期", 10),
                new SkillSeed("test-case-design", "测试用例设计", "测试", "依据需求设计等价类、边界值等测试用例", 20),
                new SkillSeed("web-testing", "Web 功能测试", "测试", "面向 Web 页面开展功能与兼容性测试", 30),
                new SkillSeed("automation-testing", "自动化测试", "测试", "使用脚本完成页面元素定位与用例维护", 40),
                new SkillSeed("performance-testing", "性能测试", "测试", "设计压测场景并分析响应时间、吞吐量等指标", 50),
                new SkillSeed("linux", "Linux 基础", "运维", "熟练使用常用命令进行服务与日志排查", 60),
                new SkillSeed("networking", "网络基础", "运维", "理解 HTTP 协议与常见网络故障排查", 70),
                new SkillSeed("python-basic", "Python 基础", "编程", "掌握语法、数据结构与常用标准库", 80),
                new SkillSeed("python-advanced", "Python 进阶", "编程", "面向对象、工程化与异步编程", 90),
                new SkillSeed("data-structures", "数据结构与算法", "算法", "掌握常用数据结构与算法解题思路", 100),
                new SkillSeed("mysql", "数据库（MySQL）", "数据", "掌握 SQL 语句与数据库表结构设计", 110),
                new SkillSeed("fastapi", "FastAPI Web 开发", "编程", "使用 FastAPI 搭建 REST 接口服务", 120),
                new SkillSeed("git", "Git 版本控制", "工程", "掌握分支、提交与团队协作流程", 130),
                new SkillSeed("algo-array-hash", "数组与哈希表", "算法", "数组、哈希表、前缀和、双指针与滑动窗口", 140),
                new SkillSeed("algo-linked-list", "链表与栈队列", "算法", "链表、栈、队列、堆与单调结构", 150),
                new SkillSeed("algo-string", "字符串处理", "算法", "字符串遍历、匹配与转换", 160),
                new SkillSeed("algo-recursion-dp", "递归与动态规划", "算法", "递归、分治、回溯与动态规划", 170),
                new SkillSeed("algo-search-sort", "排序与二分查找", "算法", "常见排序算法与二分查找", 180),
                new SkillSeed("algo-graph-tree", "树与图搜索", "算法", "广度优先、深度优先搜索与并查集", 190),
                new SkillSeed("algo-greedy-math", "贪心与数学", "算法", "贪心策略与数学推导", 200),
                new SkillSeed("algo-design", "数据结构设计", "算法", "按接口要求设计数据结构", 210),
                new SkillSeed("html-css", "HTML/CSS 基础", "前端", "使用 HTML 与 CSS 还原页面结构与样式", 220),
                new SkillSeed("javascript", "JavaScript", "前端", "掌握 JavaScript 语法与浏览器交互", 230),
                new SkillSeed("vue3", "Vue 3", "前端", "使用 Vue 3 组件化开发前端页面", 240),
                new SkillSeed("typescript", "TypeScript", "前端", "使用 TypeScript 编写类型安全的前端代码", 250),
                new SkillSeed("frontend-engineering", "前端工程化", "前端", "构建工具、模块化与前端项目组织", 260),
                new SkillSeed("java-basic", "Java 基础", "后端", "掌握 Java 语法、集合与面向对象", 270),
                new SkillSeed("spring-boot", "Spring Boot", "后端", "使用 Spring Boot 开发后端接口服务", 280),
                new SkillSeed("redis", "Redis 缓存", "后端", "使用 Redis 完成缓存与会话存储", 290),
                new SkillSeed("llm-app", "大模型应用开发", "AI", "提示词工程与大模型应用集成", 300),
                new SkillSeed("vector-db", "向量检索", "AI", "向量库与语义检索实现", 310),
                new SkillSeed("deployment", "服务部署", "工程", "把服务部署到服务器并完成基本运维", 320),
                new SkillSeed("pytorch", "PyTorch", "AI", "使用 PyTorch 搭建与训练模型", 330),
                new SkillSeed("math-stat", "数学与统计基础", "算法", "线性代数、概率论与统计基础", 340),
                new SkillSeed("data-processing", "数据处理", "数据", "数据清洗、转换与特征处理", 350),
                new SkillSeed("data-analysis", "数据分析方法", "数据", "指标体系、分析方法与结论输出", 360),
                new SkillSeed("statistics", "统计与可视化", "数据", "统计图表与可视化表达", 370),
                new SkillSeed("product-planning", "需求与产品规划", "产品", "需求分析、方案设计与优先级排序", 380),
                new SkillSeed("user-research", "用户研究", "产品", "用访谈与竞品分析理解用户", 390),
                new SkillSeed("prototyping", "原型与交互设计", "产品", "绘制原型并设计交互流程", 400)
        );
        Map<String, LearningSkill> result = new LinkedHashMap<>();
        for (SkillSeed seed : seeds) {
            LearningSkill skill = skillRepository.findByCode(seed.code()).orElseGet(LearningSkill::new);
            if (skill.getId() == null) {
                skill.setCode(seed.code());
            }
            skill.setName(seed.name());
            skill.setCategory(seed.category());
            skill.setDescription(seed.description());
            skill.setSortOrder(seed.sortOrder());
            skill.setStatus("ACTIVE");
            result.put(seed.code(), skillRepository.save(skill));
        }
        return result;
    }

    private void ensureJobRequirements(Map<String, LearningSkill> skills) {
        List<JobSeed> jobs = List.of(
                new JobSeed("python-backend", "Python 开发工程师", List.of(
                        new RequirementSeed("python-basic", 90, 1.0, 1),
                        new RequirementSeed("data-structures", 80, 0.9, 2),
                        new RequirementSeed("mysql", 70, 0.8, 3),
                        new RequirementSeed("fastapi", 75, 0.8, 4),
                        new RequirementSeed("git", 60, 0.6, 5),
                        new RequirementSeed("linux", 50, 0.5, 6),
                        new RequirementSeed("networking", 45, 0.5, 7)
                )),
                new JobSeed("software-testing", "软件测试工程师", List.of(
                        new RequirementSeed("software-testing", 85, 1.0, 1),
                        new RequirementSeed("test-case-design", 80, 0.9, 2),
                        new RequirementSeed("web-testing", 75, 0.8, 3),
                        new RequirementSeed("automation-testing", 70, 0.8, 4),
                        new RequirementSeed("performance-testing", 60, 0.6, 5),
                        new RequirementSeed("linux", 60, 0.6, 6),
                        new RequirementSeed("networking", 55, 0.5, 7)
                )),
                new JobSeed("java-backend", "Java 后端开发工程师", List.of(
                        new RequirementSeed("java-basic", 85, 1.0, 1),
                        new RequirementSeed("spring-boot", 80, 0.95, 2),
                        new RequirementSeed("mysql", 70, 0.8, 3),
                        new RequirementSeed("redis", 65, 0.7, 4),
                        new RequirementSeed("git", 60, 0.6, 5),
                        new RequirementSeed("linux", 50, 0.5, 6)
                )),
                new JobSeed("frontend", "前端开发工程师", List.of(
                        new RequirementSeed("html-css", 85, 1.0, 1),
                        new RequirementSeed("javascript", 85, 1.0, 2),
                        new RequirementSeed("vue3", 80, 0.9, 3),
                        new RequirementSeed("typescript", 70, 0.8, 4),
                        new RequirementSeed("frontend-engineering", 65, 0.7, 5),
                        new RequirementSeed("git", 55, 0.5, 6)
                )),
                new JobSeed("ai-app", "AI 应用工程师", List.of(
                        new RequirementSeed("python-basic", 85, 1.0, 1),
                        new RequirementSeed("llm-app", 80, 0.95, 2),
                        new RequirementSeed("fastapi", 75, 0.8, 3),
                        new RequirementSeed("vector-db", 70, 0.8, 4),
                        new RequirementSeed("deployment", 60, 0.6, 5),
                        new RequirementSeed("data-structures", 65, 0.6, 6)
                )),
                new JobSeed("algorithm", "算法工程师", List.of(
                        new RequirementSeed("python-basic", 90, 1.0, 1),
                        new RequirementSeed("data-structures", 85, 0.95, 2),
                        new RequirementSeed("pytorch", 80, 0.9, 3),
                        new RequirementSeed("math-stat", 75, 0.8, 4),
                        new RequirementSeed("data-processing", 70, 0.7, 5)
                )),
                new JobSeed("data-analyst", "数据分析师", List.of(
                        new RequirementSeed("data-analysis", 85, 1.0, 1),
                        new RequirementSeed("mysql", 80, 0.9, 2),
                        new RequirementSeed("statistics", 75, 0.8, 3),
                        new RequirementSeed("python-basic", 70, 0.7, 4),
                        new RequirementSeed("data-processing", 65, 0.7, 5)
                )),
                new JobSeed("product-manager", "产品经理", List.of(
                        new RequirementSeed("product-planning", 85, 1.0, 1),
                        new RequirementSeed("user-research", 75, 0.8, 2),
                        new RequirementSeed("prototyping", 70, 0.8, 3),
                        new RequirementSeed("data-analysis", 60, 0.6, 4)
                ))
        );
        for (JobSeed job : jobs) {
            Map<Long, JobSkillRequirement> existing = new LinkedHashMap<>();
            for (JobSkillRequirement item : jobRequirementRepository.findByJobCodeOrderBySortOrderAscIdAsc(job.code())) {
                existing.put(item.getSkillId(), item);
            }
            for (RequirementSeed seed : job.requirements()) {
                LearningSkill skill = skills.get(seed.skillCode());
                if (skill == null) continue;
                JobSkillRequirement target = existing.getOrDefault(skill.getId(), new JobSkillRequirement());
                target.setJobCode(job.code());
                target.setJobName(job.name());
                target.setSkillId(skill.getId());
                target.setRequiredLevel(seed.requiredLevel());
                target.setImportance(seed.importance());
                target.setRequiredFlag(Boolean.TRUE);
                target.setSortOrder(seed.sortOrder());
                jobRequirementRepository.save(target);
            }
        }
    }

    /** 补齐各岗位方向的基础课程，缺什么补什么，已有同名课程时不做修改。 */
    private void ensureStarterCourses() {
        List<CourseSeed> seeds = List.of(
                new CourseSeed("Python 编程基础", "Python 编程基础与实践",
                        List.of("开发环境与基础语法", "数据类型与流程控制", "函数、模块与异常处理"), 6),
                new CourseSeed("MySQL 数据库基础", "MySQL 数据库应用",
                        List.of("SQL 查询基础", "表设计与约束", "索引与事务"), 7),
                new CourseSeed("FastAPI 接口开发", "FastAPI Web 开发实战",
                        List.of("路由与请求参数", "数据校验与响应模型", "依赖注入与项目结构"), 8),
                new CourseSeed("Java 程序设计基础", "Java 程序设计",
                        List.of("Java 语法与面向对象", "集合与异常处理", "多线程与常用类库"), 9),
                new CourseSeed("Spring Boot 后端开发", "Spring Boot 企业级开发",
                        List.of("Spring Boot 快速入门", "数据访问与 MySQL 集成", "接口分层与 Redis 缓存"), 10),
                new CourseSeed("前端开发基础", "Web 前端开发入门",
                        List.of("HTML 页面结构", "CSS 样式与页面布局", "JavaScript 交互基础"), 11),
                new CourseSeed("Vue 3 组件开发", "Vue 3 前端项目实战",
                        List.of("Vue 3 基础语法", "组件通信与状态管理", "路由与接口联调"), 12),
                new CourseSeed("TypeScript 与前端工程化", "TypeScript 工程实践",
                        List.of("TypeScript 类型系统", "模块化与构建工具", "前端项目工程化实践"), 13),
                new CourseSeed("大模型应用开发", "大模型应用开发实战",
                        List.of("大模型与提示词基础", "大模型接口调用", "对话应用与工具调用"), 14),
                new CourseSeed("向量检索与服务部署", "向量检索与部署实践",
                        List.of("向量与语义检索", "向量库使用", "服务部署与运维"), 15),
                new CourseSeed("数据分析与可视化", "数据分析与可视化实战",
                        List.of("数据分析方法入门", "统计与图表可视化", "数据分析报告输出"), 16),
                new CourseSeed("PyTorch 深度学习入门", "PyTorch 深度学习基础",
                        List.of("数学与统计基础", "PyTorch 张量与模型", "神经网络训练实践"), 17),
                new CourseSeed("产品需求与原型设计", "产品经理入门实战",
                        List.of("需求分析与产品规划", "用户研究与竞品分析", "原型与交互设计"), 18)
        );
        Set<String> existingNames = new HashSet<>();
        for (CampusCourse course : courseRepository.findAll()) {
            existingNames.add(course.getName());
        }
        for (CourseSeed seed : seeds) {
            if (existingNames.contains(seed.name())) continue;
            CampusCourse course = new CampusCourse();
            course.setName(seed.name());
            course.setBookTitle(seed.bookTitle());
            course.setTeacherName("学习实践教研组");
            course.setDescription("学习实践课程：按章节完成学习后回写技能进度。");
            course.setEstimatedHours(2);
            course.setOwnerId(1L);
            course.setOwnerType("ADMIN");
            course.setCourseType(CampusCourse.COURSE_TYPE_PUBLIC);
            course.setAudienceType(CampusCourse.AUDIENCE_ALL);
            course.setPublishStatus(CampusCourse.STATUS_PUBLISHED);
            course.setPublishedBy(1L);
            course.setPublishTime(LocalDateTime.now());
            course.setSortOrder(seed.sortOrder());
            course = courseRepository.save(course);
            int index = 0;
            for (String title : seed.chapters()) {
                CampusCourseChapter chapter = new CampusCourseChapter();
                chapter.setCourseId(course.getId());
                chapter.setTitle(title);
                chapter.setSummary(title + "：学习内容与配套练习。");
                chapter.setRequired(Boolean.TRUE);
                chapter.setEstimatedMinutes(30);
                chapter.setSortOrder(++index);
                chapterRepository.save(chapter);
            }
        }
    }

    private void ensureCourseLinks(Map<String, LearningSkill> skills) {
        List<CourseSkillSeed> seeds = List.of(
                new CourseSkillSeed("测试基础", List.of(
                        new ContentSeed("software-testing", 0.9, 70, true, 1, "BEGINNER", null),
                        new ContentSeed("test-case-design", 0.7, 60, false, 1, "BEGINNER", null))),
                new CourseSkillSeed("Linux 与网络", List.of(
                        new ContentSeed("linux", 0.9, 65, true, 2, "BEGINNER", null),
                        new ContentSeed("networking", 0.8, 60, false, 2, "BEGINNER", null))),
                new CourseSkillSeed("Web 功能测试", List.of(
                        new ContentSeed("web-testing", 0.9, 70, true, 3, "INTERMEDIATE", "software-testing"),
                        new ContentSeed("test-case-design", 0.8, 65, false, 3, "INTERMEDIATE", "software-testing"))),
                new CourseSkillSeed("自动化测试", List.of(
                        new ContentSeed("automation-testing", 0.9, 70, true, 4, "INTERMEDIATE", null),
                        new ContentSeed("software-testing", 0.7, 60, false, 4, "INTERMEDIATE", null))),
                new CourseSkillSeed("性能测试", List.of(
                        new ContentSeed("performance-testing", 0.9, 65, true, 5, "ADVANCED", "software-testing"))),
                new CourseSkillSeed("Python 编程基础", List.of(
                        new ContentSeed("python-basic", 0.95, 70, true, 6, "BEGINNER", null),
                        new ContentSeed("python-advanced", 0.5, 60, false, 6, "BEGINNER", null))),
                new CourseSkillSeed("MySQL 数据库基础", List.of(
                        new ContentSeed("mysql", 0.9, 70, true, 7, "BEGINNER", null))),
                new CourseSkillSeed("FastAPI 接口开发", List.of(
                        new ContentSeed("fastapi", 0.9, 70, true, 8, "INTERMEDIATE", "python-basic"),
                        new ContentSeed("python-advanced", 0.6, 65, false, 8, "INTERMEDIATE", null))),
                new CourseSkillSeed("Java 程序设计基础", List.of(
                        new ContentSeed("java-basic", 0.95, 70, true, 9, "BEGINNER", null))),
                new CourseSkillSeed("Spring Boot 后端开发", List.of(
                        new ContentSeed("spring-boot", 0.95, 70, true, 10, "INTERMEDIATE", "java-basic"),
                        new ContentSeed("mysql", 0.6, 65, false, 10, "INTERMEDIATE", null),
                        new ContentSeed("redis", 0.6, 60, false, 10, "INTERMEDIATE", null))),
                new CourseSkillSeed("前端开发基础", List.of(
                        new ContentSeed("html-css", 0.95, 70, true, 11, "BEGINNER", null),
                        new ContentSeed("javascript", 0.7, 60, false, 11, "BEGINNER", null))),
                new CourseSkillSeed("Vue 3 组件开发", List.of(
                        new ContentSeed("vue3", 0.95, 70, true, 12, "INTERMEDIATE", "html-css"),
                        new ContentSeed("javascript", 0.7, 65, false, 12, "INTERMEDIATE", null))),
                new CourseSkillSeed("TypeScript 与前端工程化", List.of(
                        new ContentSeed("typescript", 0.9, 70, true, 13, "INTERMEDIATE", "javascript"),
                        new ContentSeed("frontend-engineering", 0.8, 65, false, 13, "INTERMEDIATE", null))),
                new CourseSkillSeed("大模型应用开发", List.of(
                        new ContentSeed("llm-app", 0.95, 75, true, 14, "INTERMEDIATE", "python-basic"),
                        new ContentSeed("python-basic", 0.5, 65, false, 14, "INTERMEDIATE", null))),
                new CourseSkillSeed("向量检索与服务部署", List.of(
                        new ContentSeed("vector-db", 0.9, 70, true, 15, "ADVANCED", "llm-app"),
                        new ContentSeed("deployment", 0.7, 60, false, 15, "ADVANCED", null))),
                new CourseSkillSeed("数据分析与可视化", List.of(
                        new ContentSeed("data-analysis", 0.95, 70, true, 16, "BEGINNER", null),
                        new ContentSeed("statistics", 0.8, 65, false, 16, "BEGINNER", null))),
                new CourseSkillSeed("PyTorch 深度学习入门", List.of(
                        new ContentSeed("pytorch", 0.95, 75, true, 17, "INTERMEDIATE", "python-basic"),
                        new ContentSeed("math-stat", 0.7, 65, false, 17, "INTERMEDIATE", null))),
                new CourseSkillSeed("产品需求与原型设计", List.of(
                        new ContentSeed("product-planning", 0.95, 70, true, 18, "BEGINNER", null),
                        new ContentSeed("user-research", 0.7, 60, false, 18, "BEGINNER", null),
                        new ContentSeed("prototyping", 0.7, 60, false, 18, "BEGINNER", null)))
        );
        Map<String, CampusCourse> coursesByName = new LinkedHashMap<>();
        for (CampusCourse course : courseRepository.findAll()) {
            coursesByName.putIfAbsent(course.getName(), course);
        }
        for (CourseSkillSeed seed : seeds) {
            CampusCourse course = coursesByName.get(seed.courseName());
            if (course == null) continue;
            Set<Long> desiredSkillIds = new HashSet<>();
            for (ContentSeed content : seed.contents()) {
                LearningSkill skill = skills.get(content.skillCode());
                if (skill != null) desiredSkillIds.add(skill.getId());
            }
            for (LearningContentSkill existing : contentSkillRepository
                    .findBySourceTypeAndSourceId(SOURCE_COURSE, course.getId())) {
                if (existing.getSkillId() != null && !desiredSkillIds.contains(existing.getSkillId())) {
                    contentSkillRepository.delete(existing);
                }
            }
            for (ContentSeed content : seed.contents()) {
                LearningSkill skill = skills.get(content.skillCode());
                if (skill == null) continue;
                LearningContentSkill target = contentSkillRepository
                        .findFirstBySourceTypeAndSourceIdAndSkillId(SOURCE_COURSE, course.getId(), skill.getId())
                        .orElseGet(LearningContentSkill::new);
                target.setSourceType(SOURCE_COURSE);
                target.setSourceId(course.getId());
                target.setSkillId(skill.getId());
                target.setRelevance(content.relevance());
                target.setTargetLevel(content.targetLevel());
                target.setPrimarySkill(content.primarySkill());
                target.setRecommendationOrder(content.recommendationOrder());
                target.setDifficulty(content.difficulty());
                LearningSkill prerequisite = content.prerequisiteSkillCode() == null
                        ? null
                        : skills.get(content.prerequisiteSkillCode());
                target.setPrerequisiteSkillId(prerequisite == null ? null : prerequisite.getId());
                contentSkillRepository.save(target);
            }
        }
    }

    /**
     * 章节级技能点：按章节标题/摘要匹配技能关键字，
     * 让“完成某一章”能精确回写对应技能，而不是整门课一起加分。
     */
    private void ensureChapterLinks(Map<String, LearningSkill> skills) {
        for (CampusCourse course : courseRepository.findAll()) {
            for (CampusCourseChapter chapter : chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())) {
                String text = chapter.getTitle() + " " + (chapter.getSummary() == null ? "" : chapter.getSummary());
                List<String> codes = SkillTextMatcher.matchText(text);
                Set<Long> desiredSkillIds = new HashSet<>();
                for (String code : codes) {
                    LearningSkill skill = skills.get(code);
                    if (skill != null) desiredSkillIds.add(skill.getId());
                }
                boolean staleRemoved = false;
                for (LearningContentSkill existing : contentSkillRepository
                        .findBySourceTypeAndSourceId(SOURCE_COURSE_CHAPTER, chapter.getId())) {
                    if (existing.getSkillId() != null && !desiredSkillIds.contains(existing.getSkillId())) {
                        contentSkillRepository.delete(existing);
                        staleRemoved = true;
                    }
                }
                int index = 0;
                for (String code : codes) {
                    LearningSkill skill = skills.get(code);
                    if (skill == null) continue;
                    LearningContentSkill target = contentSkillRepository
                            .findFirstBySourceTypeAndSourceIdAndSkillId(SOURCE_COURSE_CHAPTER, chapter.getId(), skill.getId())
                            .orElseGet(LearningContentSkill::new);
                    target.setSourceType(SOURCE_COURSE_CHAPTER);
                    target.setSourceId(chapter.getId());
                    target.setSkillId(skill.getId());
                    target.setRelevance(index == 0 ? 0.85 : 0.6);
                    target.setTargetLevel(60);
                    target.setPrimarySkill(index == 0);
                    target.setRecommendationOrder(0);
                    contentSkillRepository.save(target);
                    index++;
                }
                if (staleRemoved) {
                    // 关联已在上面删除，这里不再做额外处理
                }
            }
        }
    }

    /**
     * 外部精选课程：只登记标题、平台、简介与官方链接，学习时跳转原站。
     * 这里不保存任何对方的视频或讲义内容，避免版权问题。
     */
    private void ensureExternalCourses() {
        List<ExternalCourseSeed> seeds = List.of(
                new ExternalCourseSeed("Python 官方教程（中文）", "Python 官方",
                        "https://docs.python.org/zh-cn/3/tutorial/",
                        "官方入门教程，覆盖语法、数据结构、模块与异常处理。", "BEGINNER", 1),
                new ExternalCourseSeed("MDN Web 开发学习区", "MDN",
                        "https://developer.mozilla.org/zh-CN/docs/Learn",
                        "从 HTML、CSS 到 JavaScript 的前端入门学习路径。", "BEGINNER", 2),
                new ExternalCourseSeed("Vue 3 官方文档", "Vue.js 官方",
                        "https://cn.vuejs.org/guide/introduction.html",
                        "组件、响应式、路由与状态管理的官方指南。", "INTERMEDIATE", 3),
                new ExternalCourseSeed("TypeScript 官方手册", "TypeScript 官方",
                        "https://www.typescriptlang.org/docs/handbook/intro.html",
                        "类型系统、泛型与工程配置的官方手册。", "INTERMEDIATE", 4),
                new ExternalCourseSeed("菜鸟教程 Java", "菜鸟教程",
                        "https://www.runoob.com/java/java-tutorial.html",
                        "Java 语法、面向对象、集合与常用类库的中文入门教程。", "BEGINNER", 5),
                new ExternalCourseSeed("Spring 官方指南", "Spring 官方",
                        "https://spring.io/guides",
                        "Spring Boot 官方分步指南，按主题提供可运行示例。", "INTERMEDIATE", 6),
                new ExternalCourseSeed("菜鸟教程 MySQL", "菜鸟教程",
                        "https://www.runoob.com/mysql/mysql-tutorial.html",
                        "SQL 查询、表设计与索引的中文入门教程。", "BEGINNER", 7),
                new ExternalCourseSeed("FastAPI 官方中文文档", "FastAPI 官方",
                        "https://fastapi.tiangolo.com/zh/",
                        "路由、请求参数、数据校验与依赖注入的官方中文文档。", "INTERMEDIATE", 8),
                new ExternalCourseSeed("PyTorch 官方教程", "PyTorch 官方",
                        "https://pytorch.org/tutorials/",
                        "张量、模型搭建与训练流程的官方教程集合。", "INTERMEDIATE", 9),
                new ExternalCourseSeed("Git 官方书籍（中文）", "Git 官方",
                        "https://git-scm.com/book/zh/v2",
                        "《Pro Git》中文版，覆盖分支、协作与工作流。", "BEGINNER", 10),
                new ExternalCourseSeed("MDN HTTP 指南", "MDN",
                        "https://developer.mozilla.org/zh-CN/docs/Web/HTTP",
                        "HTTP 报文、方法、状态码与缓存的系统讲解。", "BEGINNER", 11),
                new ExternalCourseSeed("菜鸟教程 Linux", "菜鸟教程",
                        "https://www.runoob.com/linux/linux-tutorial.html",
                        "Linux 常用命令、文件权限与进程管理中文教程。", "BEGINNER", 12),
                new ExternalCourseSeed("Kaggle Learn: Pandas", "Kaggle",
                        "https://www.kaggle.com/learn/pandas",
                        "用 Pandas 做数据清洗、筛选与聚合的实战课程。", "BEGINNER", 13),
                new ExternalCourseSeed("Kaggle Learn: 数据可视化", "Kaggle",
                        "https://www.kaggle.com/learn/data-visualization",
                        "用图表表达数据结论的可视化入门课程。", "BEGINNER", 14),
                new ExternalCourseSeed("Hugging Face 课程", "Hugging Face",
                        "https://huggingface.co/learn",
                        "大模型与 NLP 方向的官方开放课程。", "INTERMEDIATE", 15),
                new ExternalCourseSeed("Redis 官方文档", "Redis 官方",
                        "https://redis.io/docs/",
                        "Redis 数据结构、命令与缓存设计官方文档。", "BEGINNER", 16),
                new ExternalCourseSeed("Docker 官方入门", "Docker 官方",
                        "https://docs.docker.com/get-started/",
                        "镜像、容器与编排的官方入门指南。", "INTERMEDIATE", 17)
        );
        for (ExternalCourseSeed seed : seeds) {
            ExternalCourse course = externalCourseRepository.findByTitle(seed.title())
                    .orElseGet(ExternalCourse::new);
            if (course.getId() == null) {
                course.setTitle(seed.title());
            }
            course.setProvider(seed.provider());
            course.setUrl(seed.url());
            course.setDescription(seed.description());
            course.setLevel(seed.level());
            course.setFree(Boolean.TRUE);
            course.setSortOrder(seed.sortOrder());
            course.setStatus(ExternalCourse.STATUS_ACTIVE);
            externalCourseRepository.save(course);
        }
    }

    private void ensureExternalCourseLinks(Map<String, LearningSkill> skills) {
        List<ExternalLinkSeed> seeds = List.of(
                new ExternalLinkSeed("Python 官方教程（中文）", List.of(
                        new ContentSeed("python-basic", 0.6, 65, true, 1, "BEGINNER", null),
                        new ContentSeed("python-advanced", 0.45, 60, false, 1, "BEGINNER", null))),
                new ExternalLinkSeed("MDN Web 开发学习区", List.of(
                        new ContentSeed("html-css", 0.6, 65, true, 1, "BEGINNER", null),
                        new ContentSeed("javascript", 0.5, 60, false, 1, "BEGINNER", null))),
                new ExternalLinkSeed("Vue 3 官方文档", List.of(
                        new ContentSeed("vue3", 0.6, 65, true, 1, "INTERMEDIATE", "html-css"),
                        new ContentSeed("javascript", 0.45, 60, false, 1, "INTERMEDIATE", null))),
                new ExternalLinkSeed("TypeScript 官方手册", List.of(
                        new ContentSeed("typescript", 0.6, 65, true, 1, "INTERMEDIATE", "javascript"),
                        new ContentSeed("frontend-engineering", 0.45, 60, false, 1, "INTERMEDIATE", null))),
                new ExternalLinkSeed("菜鸟教程 Java", List.of(
                        new ContentSeed("java-basic", 0.6, 65, true, 1, "BEGINNER", null))),
                new ExternalLinkSeed("Spring 官方指南", List.of(
                        new ContentSeed("spring-boot", 0.6, 65, true, 1, "INTERMEDIATE", "java-basic"),
                        new ContentSeed("java-basic", 0.4, 60, false, 1, "INTERMEDIATE", null))),
                new ExternalLinkSeed("菜鸟教程 MySQL", List.of(
                        new ContentSeed("mysql", 0.6, 65, true, 1, "BEGINNER", null))),
                new ExternalLinkSeed("FastAPI 官方中文文档", List.of(
                        new ContentSeed("fastapi", 0.6, 65, true, 1, "INTERMEDIATE", "python-basic"),
                        new ContentSeed("python-basic", 0.4, 60, false, 1, "INTERMEDIATE", null))),
                new ExternalLinkSeed("PyTorch 官方教程", List.of(
                        new ContentSeed("pytorch", 0.6, 65, true, 1, "INTERMEDIATE", "python-basic"),
                        new ContentSeed("math-stat", 0.4, 60, false, 1, "INTERMEDIATE", null))),
                new ExternalLinkSeed("Git 官方书籍（中文）", List.of(
                        new ContentSeed("git", 0.6, 65, true, 1, "BEGINNER", null))),
                new ExternalLinkSeed("MDN HTTP 指南", List.of(
                        new ContentSeed("networking", 0.6, 65, true, 1, "BEGINNER", null))),
                new ExternalLinkSeed("菜鸟教程 Linux", List.of(
                        new ContentSeed("linux", 0.6, 65, true, 1, "BEGINNER", null))),
                new ExternalLinkSeed("Kaggle Learn: Pandas", List.of(
                        new ContentSeed("data-processing", 0.6, 65, true, 1, "BEGINNER", "python-basic"),
                        new ContentSeed("python-basic", 0.4, 60, false, 1, "BEGINNER", null))),
                new ExternalLinkSeed("Kaggle Learn: 数据可视化", List.of(
                        new ContentSeed("statistics", 0.6, 65, true, 1, "BEGINNER", null),
                        new ContentSeed("data-analysis", 0.5, 60, false, 1, "BEGINNER", null))),
                new ExternalLinkSeed("Hugging Face 课程", List.of(
                        new ContentSeed("llm-app", 0.6, 65, true, 1, "INTERMEDIATE", "python-basic"),
                        new ContentSeed("pytorch", 0.45, 60, false, 1, "INTERMEDIATE", null))),
                new ExternalLinkSeed("Redis 官方文档", List.of(
                        new ContentSeed("redis", 0.6, 65, true, 1, "BEGINNER", null))),
                new ExternalLinkSeed("Docker 官方入门", List.of(
                        new ContentSeed("deployment", 0.6, 65, true, 1, "INTERMEDIATE", null)))
        );
        for (ExternalLinkSeed seed : seeds) {
            ExternalCourse course = externalCourseRepository.findByTitle(seed.title()).orElse(null);
            if (course == null) continue;
            Set<Long> desiredSkillIds = new HashSet<>();
            for (ContentSeed content : seed.contents()) {
                LearningSkill skill = skills.get(content.skillCode());
                if (skill != null) desiredSkillIds.add(skill.getId());
            }
            for (LearningContentSkill existing : contentSkillRepository
                    .findBySourceTypeAndSourceId(SOURCE_EXTERNAL_COURSE, course.getId())) {
                if (existing.getSkillId() != null && !desiredSkillIds.contains(existing.getSkillId())) {
                    contentSkillRepository.delete(existing);
                }
            }
            for (ContentSeed content : seed.contents()) {
                LearningSkill skill = skills.get(content.skillCode());
                if (skill == null) continue;
                LearningContentSkill target = contentSkillRepository
                        .findFirstBySourceTypeAndSourceIdAndSkillId(SOURCE_EXTERNAL_COURSE, course.getId(), skill.getId())
                        .orElseGet(LearningContentSkill::new);
                target.setSourceType(SOURCE_EXTERNAL_COURSE);
                target.setSourceId(course.getId());
                target.setSkillId(skill.getId());
                target.setRelevance(content.relevance());
                target.setTargetLevel(content.targetLevel());
                target.setPrimarySkill(content.primarySkill());
                target.setRecommendationOrder(content.recommendationOrder());
                target.setDifficulty(content.difficulty());
                LearningSkill prerequisite = content.prerequisiteSkillCode() == null
                        ? null
                        : skills.get(content.prerequisiteSkillCode());
                target.setPrerequisiteSkillId(prerequisite == null ? null : prerequisite.getId());
                contentSkillRepository.save(target);
            }
        }
    }

    private record SkillSeed(String code, String name, String category, String description, int sortOrder) { }

    private record JobSeed(String code, String name, List<RequirementSeed> requirements) { }

    private record RequirementSeed(String skillCode, int requiredLevel, double importance, int sortOrder) { }

    private record CourseSeed(String name, String bookTitle, List<String> chapters, int sortOrder) { }

    private record CourseSkillSeed(String courseName, List<ContentSeed> contents) { }

    private record ContentSeed(String skillCode, double relevance, int targetLevel, boolean primarySkill, int recommendationOrder, String difficulty, String prerequisiteSkillCode) { }

    private record ExternalCourseSeed(String title, String provider, String url, String description, String level, int sortOrder) { }

    private record ExternalLinkSeed(String title, List<ContentSeed> contents) { }
}