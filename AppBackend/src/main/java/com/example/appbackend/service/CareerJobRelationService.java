package com.example.appbackend.service;

import com.example.appbackend.entity.CareerJobRelation;
import com.example.appbackend.repository.CareerJobRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** 岗位关系配置（管理端维护，学生端只读）。 */
@Service
public class CareerJobRelationService {

    private static final Set<String> RELATION_TYPES =
            Set.of(CareerJobRelation.TYPE_PROMOTION, CareerJobRelation.TYPE_TRANSFER,
                    CareerJobRelation.TYPE_RELATED, CareerJobRelation.TYPE_BRANCH);
    private static final Set<String> LINE_TYPES = Set.of("SOLID", "DASHED", "THIN", "GLOW");

    private final CareerJobRelationRepository repository;

    public CareerJobRelationService(CareerJobRelationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CareerJobRelation> listAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<CareerJobRelation> listEnabled() {
        return repository.findByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public CareerJobRelation create(CareerJobRelation payload) {
        CareerJobRelation entity = new CareerJobRelation();
        apply(entity, payload);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder((int) repository.count());
        }
        return repository.save(entity);
    }

    @Transactional
    public CareerJobRelation update(Long id, CareerJobRelation payload) {
        CareerJobRelation entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("岗位关系不存在"));
        apply(entity, payload);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("岗位关系不存在");
        }
        repository.deleteById(id);
    }

    private void apply(CareerJobRelation entity, CareerJobRelation payload) {
        if (payload == null) {
            throw new IllegalArgumentException("岗位关系内容不能为空");
        }
        String source = trim(payload.getSourceJobId());
        String target = trim(payload.getTargetJobId());
        if (source.isEmpty() || target.isEmpty()) {
            throw new IllegalArgumentException("必须选择源岗位与目标岗位");
        }
        if (source.equals(target)) {
            throw new IllegalArgumentException("源岗位与目标岗位不能相同");
        }
        entity.setSourceJobId(source);
        entity.setTargetJobId(target);
        entity.setRelationType(normalizeType(payload.getRelationType()));
        entity.setRelationName(trim(payload.getRelationName()));
        entity.setDescription(trim(payload.getDescription()));
        entity.setReusableSkills(trim(payload.getReusableSkills()));
        entity.setMissingSkills(trim(payload.getMissingSkills()));
        entity.setLineType(normalizeLineType(payload.getLineType(), entity.getRelationType()));
        if (payload.getRecommendation() != null) {
            int value = payload.getRecommendation();
            entity.setRecommendation(Math.max(0, Math.min(100, value)));
        }
        if (payload.getEnabled() != null) {
            entity.setEnabled(payload.getEnabled());
        }
        if (payload.getSortOrder() != null) {
            entity.setSortOrder(payload.getSortOrder());
        }
    }

    private String normalizeType(String raw) {
        String value = trim(raw).toUpperCase();
        return RELATION_TYPES.contains(value) ? value : CareerJobRelation.TYPE_RELATED;
    }

    private String normalizeLineType(String raw, String relationType) {
        String value = trim(raw).toUpperCase();
        if (LINE_TYPES.contains(value)) {
            return value;
        }
        return CareerJobRelation.TYPE_PROMOTION.equals(relationType) ? "SOLID" : "DASHED";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
