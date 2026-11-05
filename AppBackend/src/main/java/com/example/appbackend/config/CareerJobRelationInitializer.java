package com.example.appbackend.config;

import com.example.appbackend.entity.CareerJobRelation;
import com.example.appbackend.repository.CareerJobRelationRepository;
import com.example.appbackend.service.CareerNebulaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位关系初始数据。
 *
 * 只在「关系表为空」且星图里确实存在这两个岗位时写入，管理端维护过的内容不会被覆盖。
 * 这些关系只是让职业路径图谱第一次打开就有内容，管理员可以随时在管理端修改或删除。
 */
@Component
@Order(217)
public class CareerJobRelationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CareerJobRelationInitializer.class);

    private final CareerJobRelationRepository repository;
    private final CareerNebulaService nebulaService;

    public CareerJobRelationInitializer(CareerJobRelationRepository repository,
                                        CareerNebulaService nebulaService) {
        this.repository = repository;
        this.nebulaService = nebulaService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        // 按岗位名称解析星图 id：管理端换过节点 id 也不会失效
        Map<String, String> jobIdsByName = nebulaJobIdsByName();
        List<CareerJobRelation> seeds = new ArrayList<>();
        addSeed(seeds, jobIdsByName, "Java 开发工程师", "AI 应用工程师",
                CareerJobRelation.TYPE_BRANCH, "发展分支",
                "后端开发能力可以向 AI 应用方向延伸，重点补大模型调用、检索与向量库相关技能。",
                82, "GLOW", 1);
        addSeed(seeds, jobIdsByName, "数据分析师", "AI 应用工程师",
                CareerJobRelation.TYPE_BRANCH, "发展分支",
                "数据分析能力可以向 AI 应用延伸，补上大模型调用、RAG 与向量数据库后，可以承担数据驱动的 AI 应用开发。",
                74, "GLOW", 2);
        addSeed(seeds, jobIdsByName, "前端开发工程师", "Java 开发工程师",
                CareerJobRelation.TYPE_TRANSFER, "横向转岗",
                "前端转后端需要补服务端语言、数据库与接口设计能力，前端工程与联调经验可以复用。",
                72, "DASHED", 3);
        addSeed(seeds, jobIdsByName, "软件测试工程师", "Java 开发工程师",
                CareerJobRelation.TYPE_TRANSFER, "横向转岗",
                "测试岗位熟悉业务流程与接口，转后端开发需要补强编程与框架能力。",
                68, "DASHED", 4);
        if (seeds.isEmpty()) {
            return;
        }
        repository.saveAll(seeds);
        log.info("已写入岗位关系初始数据 {} 条", seeds.size());
    }

    private void addSeed(List<CareerJobRelation> seeds, Map<String, String> jobIdsByName,
                         String sourceName, String targetName, String type, String name,
                         String description, int recommendation, String lineType, int sortOrder) {
        String source = jobIdsByName.get(sourceName);
        String target = jobIdsByName.get(targetName);
        if (source == null || target == null) {
            return;
        }
        seeds.add(relation(source, target, type, name, description, recommendation, lineType, sortOrder));
    }

    private CareerJobRelation relation(String source, String target, String type, String name,
                                       String description, int recommendation, String lineType, int sortOrder) {
        CareerJobRelation entity = new CareerJobRelation();
        entity.setSourceJobId(source);
        entity.setTargetJobId(target);
        entity.setRelationType(type);
        entity.setRelationName(name);
        entity.setDescription(description);
        entity.setRecommendation(recommendation);
        entity.setLineType(lineType);
        entity.setEnabled(true);
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private Map<String, String> nebulaJobIdsByName() {
        Map<String, String> ids = new HashMap<>();
        Map<String, Object> map = nebulaService.getMap();
        Object raw = map == null ? null : map.get("careers");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row && row.get("id") != null && row.get("name") != null) {
                    String id = String.valueOf(row.get("id")).trim();
                    String name = String.valueOf(row.get("name")).trim();
                    if (!id.isEmpty() && !name.isEmpty()) {
                        ids.putIfAbsent(name, id);
                    }
                }
            }
        }
        return ids;
    }
}
