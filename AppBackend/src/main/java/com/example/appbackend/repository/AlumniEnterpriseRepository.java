package com.example.appbackend.repository;

import com.example.appbackend.entity.AlumniEnterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlumniEnterpriseRepository extends JpaRepository<AlumniEnterprise, Long> {

    /** 管理端列表：全部企业，按排序值升序 */
    List<AlumniEnterprise> findAllByOrderBySortOrderAscIdAsc();

    /** 管理端搜索：按企业名称模糊匹配 */
    List<AlumniEnterprise> findByNameContainingOrderBySortOrderAscIdAsc(String keyword);

    /** 学生端列表：只返回展示中的企业 */
    List<AlumniEnterprise> findByEnabledTrueOrderBySortOrderAscIdAsc();
}
