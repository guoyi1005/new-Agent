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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Set<String> jobIds = nebulaJobIds();
        List<CareerJobRelation> seeds = List.of(
                relation("java", "ai", CareerJobRelation.TYPE_TRANSFER, "横向转岗",
                        "后端开发能力可以向 AI 应用方向复用，重点补大模型调用、检索与向量库相关技能。",
                        82, "DASHED", 1),
                relation("testing", "java", CareerJobRelation.TYPE_TRANSFER, "横向转岗",
                        "测试岗位熟悉业务流程与接口，转后端开发需要补强编程与框架能力。",
                        68, "DASHED", 2),
                relation("frontend", "java", CareerJobRelation.TYPE_RELATED, "相近岗位",
                        "前端与后端属于同一条研发链路，工程化与接口协作经验可以复用。",
                        61, "THIN", 3)
        );
        List<CareerJobRelation> valid = seeds.stream()
                .filter((item) -> jobIds.contains(item.getSourceJobId()) && jobIds.contains(item.getTargetJobId()))
                .toList();
        if (valid.isEmpty()) {
            return;
        }
        repository.saveAll(valid);
        log.info("已写入岗位关系初始数据 {} 条", valid.size());
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

    private Set<String> nebulaJobIds() {
        Set<String> ids = new HashSet<>();
        Map<String, Object> map = nebulaService.getMap();
        Object raw = map == null ? null : map.get("careers");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row && row.get("id") != null) {
                    ids.add(String.valueOf(row.get("id")).trim());
                }
            }
        }
        return ids;
    }
}
