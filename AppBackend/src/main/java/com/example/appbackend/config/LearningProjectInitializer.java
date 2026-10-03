package com.example.appbackend.config;

import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningProject;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningProjectRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 岗位实战任务：把技能落成可交付的实战项目，再通过 learning_content_skill(PROJECT)
 * 与技能关联，使「项目实训 - 岗位实战任务」能随目标岗位的技能要求自动变化。
 *
 * 幂等：项目按 code 去重，技能关联按 sourceType + sourceId + skillId 去重。
 * 依赖 LearningTaxonomyInitializer 先建好技能字典，因此 Order 排在它之后。
 */
@Component
@Order(211)
public class LearningProjectInitializer implements ApplicationRunner {

    private static final String SOURCE_PROJECT = "PROJECT";

    private final LearningProjectRepository projectRepository;
    private final LearningSkillRepository skillRepository;
    private final LearningContentSkillRepository contentSkillRepository;

    public LearningProjectInitializer(
            LearningProjectRepository projectRepository,
            LearningSkillRepository skillRepository,
            LearningContentSkillRepository contentSkillRepository
    ) {
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.contentSkillRepository = contentSkillRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, LearningSkill> skills = skillRepository.findAll().stream()
                .collect(Collectors.toMap(LearningSkill::getCode, Function.identity(), (left, right) -> left));
        if (skills.isEmpty()) return;
        for (ProjectSeed seed : projectSeeds()) {
            LearningProject project = projectRepository.findByCode(seed.code()).orElseGet(LearningProject::new);
            if (project.getId() == null) {
                project.setCode(seed.code());
            }
            project.setTitle(seed.title());
            project.setSummary(seed.summary());
            project.setObjective(seed.objective());
            project.setDeliverable(seed.deliverable());
            project.setDifficulty(seed.difficulty());
            project.setEstimatedHours(seed.hours());
            project.setSortOrder(seed.sortOrder());
            project.setStatus(LearningProject.STATUS_ACTIVE);
            LearningProject saved = projectRepository.save(project);
            ensureLinks(saved, seed.skillCodes(), skills);
        }
    }

    private void ensureLinks(LearningProject project, List<String> skillCodes, Map<String, LearningSkill> skills) {
        Set<Long> desired = new HashSet<>();
        for (String code : skillCodes) {
            LearningSkill skill = skills.get(code);
            if (skill != null) desired.add(skill.getId());
        }
        for (LearningContentSkill existing : contentSkillRepository
                .findBySourceTypeAndSourceId(SOURCE_PROJECT, project.getId())) {
            if (existing.getSkillId() != null && !desired.contains(existing.getSkillId())) {
                contentSkillRepository.delete(existing);
            }
        }
        int order = 1;
        for (String code : skillCodes) {
            LearningSkill skill = skills.get(code);
            if (skill == null) continue;
            LearningContentSkill target = contentSkillRepository
                    .findFirstBySourceTypeAndSourceIdAndSkillId(SOURCE_PROJECT, project.getId(), skill.getId())
                    .orElseGet(LearningContentSkill::new);
            target.setSourceType(SOURCE_PROJECT);
            target.setSourceId(project.getId());
            target.setSkillId(skill.getId());
            target.setRelevance(order == 1 ? 0.85 : 0.6);
            target.setTargetLevel(order == 1 ? 70 : 60);
            target.setPrimarySkill(order == 1);
            target.setRecommendationOrder(project.getSortOrder());
            target.setDifficulty(project.getDifficulty());
            target.setPrerequisiteSkillId(null);
            contentSkillRepository.save(target);
            order++;
        }
    }

    private List<ProjectSeed> projectSeeds() {
        return List.of(
                new ProjectSeed("project-test-plan", "校园二手平台测试计划与缺陷报告",
                        "为一个完整业务系统制定测试计划，并跟踪缺陷闭环",
                        "根据需求梳理测试范围、测试策略与准入准出标准，提交可复现的缺陷记录",
                        "一份测试计划 + 至少 5 条带复现步骤的缺陷记录",
                        "BEGINNER", 6, 1, List.of("software-testing")),
                new ProjectSeed("project-test-case", "登录模块测试用例设计",
                        "用等价类、边界值方法覆盖真实登录场景",
                        "针对账号、密码、验证码三类输入设计正向与异常用例，保证用例可执行可回归",
                        "一份含 20 条以上用例、覆盖边界与异常场景的用例表",
                        "BEGINNER", 4, 2, List.of("test-case-design")),
                new ProjectSeed("project-web-test", "校园网 Web 端功能测试实战",
                        "对一个真实 Web 系统的核心流程做完整功能测试",
                        "围绕登录、查询、提交三个主流程执行测试，记录环境、步骤与结果",
                        "一份功能测试报告 + 缺陷列表",
                        "INTERMEDIATE", 6, 3, List.of("web-testing")),
                new ProjectSeed("project-ui-automation", "自动化用例覆盖登录与下单主流程",
                        "把手工回归用例改写成可重复执行的自动化脚本",
                        "使用元素定位与断言把主流程自动化，并支持一键回归",
                        "一套可运行的自动化脚本 + 执行报告",
                        "INTERMEDIATE", 8, 4, List.of("automation-testing")),
                new ProjectSeed("project-perf-test", "选课接口性能压测与调优分析",
                        "定位高并发下的接口瓶颈并给出优化建议",
                        "设计压测场景，采集响应时间与吞吐量数据，分析瓶颈并验证优化效果",
                        "一份含指标图表的性能测试与调优报告",
                        "ADVANCED", 8, 5, List.of("performance-testing", "networking")),
                new ProjectSeed("project-linux-deploy", "在 Linux 服务器部署并排查后端服务",
                        "掌握从部署到日志排查的完整运维流程",
                        "完成服务上传、进程管理、端口放通与日志定位，保证服务可稳定访问",
                        "一份部署脚本与排查记录",
                        "INTERMEDIATE", 6, 6, List.of("linux")),
                new ProjectSeed("project-http-debug", "抓包分析一次完整接口请求",
                        "看懂请求与响应，定位常见网络问题",
                        "抓取一个真实接口的请求响应，分析方法、状态码、报文头与耗时",
                        "一份接口抓包分析记录",
                        "BEGINNER", 3, 7, List.of("networking")),
                new ProjectSeed("project-python-report", "用 Python 生成成绩统计报表",
                        "用脚本替代手工统计，产出可复用的报表",
                        "读取成绩数据，完成清洗、统计与分组，输出控制台报表与文件",
                        "一个可复用的统计脚本 + 样例输出",
                        "BEGINNER", 5, 8, List.of("python-basic")),
                new ProjectSeed("project-python-package", "把脚本重构为可复用的 Python 包",
                        "从单文件脚本进阶到工程化组织",
                        "拆分模块、补充异常处理与日志，并支持命令行调用",
                        "一个带目录结构、可安装调用的 Python 包",
                        "INTERMEDIATE", 6, 9, List.of("python-advanced")),
                new ProjectSeed("project-algo-ranking", "用数据结构实现实时排行榜",
                        "把算法知识用到真实的数据组织问题上",
                        "选择合适的数据结构支持插入、更新与 TopN 查询，并分析复杂度",
                        "排行榜实现代码 + 复杂度分析",
                        "INTERMEDIATE", 6, 10, List.of("data-structures")),
                new ProjectSeed("project-mysql-schema", "选课系统表结构与查询优化",
                        "设计能支撑真实业务的关系表并优化查询",
                        "完成实体建模、建表语句与索引设计，针对慢查询给出优化前后对比",
                        "建表脚本 + 关键 SQL 与优化说明",
                        "INTERMEDIATE", 6, 11, List.of("mysql")),
                new ProjectSeed("project-fastapi-service", "用 FastAPI 提供校园服务接口",
                        "把业务能力封装成规范的 REST 接口",
                        "实现增删改查接口，补充参数校验、错误处理与接口文档",
                        "一套可运行的 FastAPI 服务 + 接口文档",
                        "INTERMEDIATE", 8, 12, List.of("fastapi", "python-basic")),
                new ProjectSeed("project-git-collab", "用 Git 分支协作完成一次功能合并",
                        "掌握团队协作中的分支与冲突处理",
                        "按特性开分支开发，处理一次真实冲突并完成代码评审式合并",
                        "分支记录与合并说明",
                        "BEGINNER", 3, 13, List.of("git")),
                new ProjectSeed("project-java-console", "Java 学生信息管理程序",
                        "用面向对象方式组织一个完整小程序",
                        "设计实体与业务类，实现增删改查与输入校验",
                        "可运行的控制台程序源码",
                        "BEGINNER", 6, 14, List.of("java-basic")),
                new ProjectSeed("project-springboot-api", "Spring Boot 课程预约后端服务",
                        "完成从建表到接口上线的后端主流程",
                        "实现分层结构、数据库访问、参数校验与统一异常处理",
                        "可运行的 Spring Boot 服务 + 接口说明",
                        "INTERMEDIATE", 10, 15, List.of("spring-boot", "mysql")),
                new ProjectSeed("project-redis-cache", "给热点接口加上 Redis 缓存",
                        "用缓存解决热点读压力并验证效果",
                        "设计缓存键与过期策略，处理缓存穿透，并对比优化前后响应时间",
                        "缓存改造代码 + 效果对比",
                        "INTERMEDIATE", 5, 16, List.of("redis")),
                new ProjectSeed("project-static-page", "还原校园活动静态页面",
                        "按设计稿还原布局与响应式效果",
                        "完成结构语义化、栅格布局与移动端适配，保证主流浏览器一致",
                        "一个响应式静态页面",
                        "BEGINNER", 5, 17, List.of("html-css")),
                new ProjectSeed("project-js-todo", "原生 JS 待办清单交互",
                        "不依赖框架完成完整的交互逻辑",
                        "实现新增、完成、删除、筛选与本地存储",
                        "一个可用的原生 JS 交互页面",
                        "BEGINNER", 5, 18, List.of("javascript")),
                new ProjectSeed("project-vue-activity", "Vue 3 校园活动列表与详情",
                        "用组件化方式组织页面与数据流",
                        "拆分为列表、卡片与详情组件，处理加载、空状态与错误状态",
                        "一个 Vue 3 页面模块源码",
                        "INTERMEDIATE", 8, 19, List.of("vue3", "javascript")),
                new ProjectSeed("project-ts-refactor", "给前端项目补上 TypeScript 类型",
                        "用类型系统降低联调与重构成本",
                        "为接口数据、组件属性与工具函数补充类型，消除隐式 any",
                        "类型改造后的项目与类型说明",
                        "INTERMEDIATE", 6, 20, List.of("typescript")),
                new ProjectSeed("project-fe-scaffold", "搭建前端工程化构建流程",
                        "把零散文件组织成可协作的工程",
                        "配置构建、环境变量、代码规范与打包分析",
                        "一套可复用的前端工程骨架",
                        "INTERMEDIATE", 6, 21, List.of("frontend-engineering")),
                new ProjectSeed("project-llm-assistant", "大模型校园问答助手",
                        "把大模型能力落到一个真实问答场景",
                        "设计提示词与上下文组织，处理多轮对话与异常返回",
                        "一个可对话的问答助手 + 提示词说明",
                        "INTERMEDIATE", 10, 22, List.of("llm-app", "python-basic")),
                new ProjectSeed("project-vector-search", "课程资料向量检索服务",
                        "让资料可以被语义检索到",
                        "完成文本切分、向量化与相似度检索，并接入问答流程",
                        "一个可检索的向量检索服务",
                        "ADVANCED", 10, 23, List.of("vector-db", "llm-app")),
                new ProjectSeed("project-container-deploy", "接口服务容器化与部署",
                        "把本地服务变成可交付的线上服务",
                        "编写镜像构建文件与编排配置，完成环境变量与端口暴露",
                        "容器化配置 + 部署文档",
                        "INTERMEDIATE", 6, 24, List.of("deployment")),
                new ProjectSeed("project-pytorch-classifier", "PyTorch 图像分类小模型",
                        "走通数据处理到模型评估的完整链路",
                        "完成数据集加载、模型搭建、训练循环与指标评估",
                        "训练脚本 + 评估结果",
                        "INTERMEDIATE", 10, 25, List.of("pytorch", "math-stat")),
                new ProjectSeed("project-data-cleaning", "清洗并合并多份校园数据表",
                        "把脏数据整理成可分析的数据集",
                        "处理缺失值、重复值与格式不一致，完成多表关联",
                        "清洗后的数据集 + 处理说明",
                        "INTERMEDIATE", 6, 26, List.of("data-processing", "python-basic")),
                new ProjectSeed("project-data-report", "校园消费数据分析报告",
                        "从数据中得出可解释的结论",
                        "完成指标定义、分组对比与结论说明，避免只看图表不解释",
                        "一份含结论与建议的数据分析报告",
                        "INTERMEDIATE", 8, 27, List.of("data-analysis", "statistics")),
                new ProjectSeed("project-stats-visual", "用统计方法分析校园问卷",
                        "用统计口径支撑结论而不是凭感觉",
                        "完成描述统计、分组差异检验与图表表达",
                        "统计分析结果与可视化图表",
                        "INTERMEDIATE", 6, 28, List.of("statistics", "data-analysis")),
                new ProjectSeed("project-prd", "校园二手平台需求文档与优先级",
                        "把模糊想法写成可执行的需求",
                        "梳理用户场景、功能清单与优先级，明确验收口径",
                        "一份需求文档 + 功能优先级表",
                        "BEGINNER", 5, 29, List.of("product-planning")),
                new ProjectSeed("project-user-interview", "用户访谈与洞察输出",
                        "用真实访谈替代主观猜测",
                        "设计访谈提纲，完成 5 次访谈并归纳共性需求",
                        "访谈提纲 + 访谈记录 + 洞察结论",
                        "BEGINNER", 6, 30, List.of("user-research")),
                new ProjectSeed("project-prototype", "核心流程交互原型",
                        "把需求落成可点击的交互方案",
                        "完成核心流程的页面结构与交互稿，标注关键状态",
                        "一份可点击的交互原型",
                        "INTERMEDIATE", 8, 31, List.of("prototyping"))
        );
    }

    private record ProjectSeed(String code, String title, String summary, String objective,
                               String deliverable, String difficulty, int hours, int sortOrder,
                               List<String> skillCodes) { }
}
