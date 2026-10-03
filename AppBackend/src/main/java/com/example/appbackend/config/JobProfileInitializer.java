package com.example.appbackend.config;

import com.example.appbackend.entity.JobProfile;
import com.example.appbackend.repository.JobProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 岗位基本信息初始内容。
 *
 * 说明：
 * 1. job_code 与 LearningTaxonomyInitializer 写入的岗位技能要求保持一致，两边才能算匹配度；
 * 2. 岗位方向、类型、职责与发展建议沿用岗位探索页已经展示的文案，没有新增杜撰内容；
 * 3. 只在表为空时写入，后续管理端维护过的内容不会被覆盖。
 */
@Component
@Order(215)
public class JobProfileInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JobProfileInitializer.class);

    private final JobProfileRepository repository;

    public JobProfileInitializer(JobProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                profile("python-backend", "Python 开发工程师", "后端开发 / 服务端", "应届 / 实习",
                        "负责后端接口、业务服务与数据处理，把产品需求落成可上线的服务。",
                        "业务接口开发；服务端模块实现；数据库表设计",
                        "先把 FastAPI 跑通一个完整项目，再补 MySQL 索引与查询优化。", 1),
                profile("software-testing", "软件测试工程师", "软件测试 / 质量", "应届 / 实习",
                        "负责功能与性能测试，保证上线质量。",
                        "用例设计与执行；数据校验；问题定位",
                        "先补性能测试与抓包分析，再尝试写一部分自动化用例。", 2),
                profile("java-backend", "Java 后端开发工程师", "后端开发 / 服务端", "应届 / 实习",
                        "用 Java 技术栈开发业务系统，负责接口、数据与稳定性。",
                        "业务接口开发；服务模块搭建；数据层优化",
                        "先完成一个 Spring Boot 项目，再补 Redis 缓存与接口优化。", 3),
                profile("frontend", "前端开发工程师", "前端开发 / Web", "应届 / 实习",
                        "负责页面还原与交互实现，把设计稿变成稳定好用的界面。",
                        "页面与组件开发；类型化改造；工程配置与构建",
                        "先补齐 Vue3 组件化，再把 TypeScript 用进现有项目。", 4),
                profile("ai-app", "AI 应用工程师", "AI 应用 / 后端", "实习",
                        "把大模型能力接进真实业务，负责提示词、检索与对外接口。",
                        "RAG 开发；提示词设计；接口开发",
                        "先做一个 RAG 小项目，把提示词、检索和接口串成完整链路。", 5),
                profile("algorithm", "算法工程师", "算法工程 / 模型", "实习",
                        "用模型解决业务问题，负责数据、训练与效果评估。",
                        "模型训练；数据清洗与特征；效果调优与上线",
                        "先把 PyTorch 训练流程跑通，再用一个公开数据集做完整实验。", 6),
                profile("data-analyst", "数据分析师", "数据分析 / 业务", "应届 / 实习",
                        "用数据和看板回答业务问题，支撑活动与产品决策。",
                        "多表取数与核对；看板搭建；业务复盘",
                        "先补齐一个完整的看板作品，再练习把结论讲成业务语言。", 7),
                profile("product-manager", "产品经理", "产品经理 / 业务", "应届 / 实习",
                        "定义做什么和为什么做，推动需求从想法走到上线。",
                        "需求梳理；原型与方案；上线数据复盘",
                        "先补原型与数据分析，再完整跟一个小功能的落地过程。", 8)
        ));
        log.info("已写入岗位基本信息 {} 条", repository.count());
    }

    private JobProfile profile(String code, String name, String direction, String type,
                               String summary, String responsibilities, String advice, int sortOrder) {
        JobProfile entity = new JobProfile();
        entity.setCode(code);
        entity.setName(name);
        entity.setDirection(direction);
        entity.setType(type);
        entity.setSummary(summary);
        entity.setResponsibilities(responsibilities);
        entity.setAdvice(advice);
        entity.setStatus(JobProfile.STATUS_ACTIVE);
        entity.setSortOrder(sortOrder);
        return entity;
    }
}
