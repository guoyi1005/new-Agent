package com.example.appbackend.repository;

import com.example.appbackend.entity.CampusRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampusRecruitmentRepository extends JpaRepository<CampusRecruitment, Long> {

    /** 管理端列表：全部条目，按排序值升序 */
    List<CampusRecruitment> findAllByOrderBySortOrderAscIdAsc();

    /** 管理端搜索：按活动/岗位名称模糊匹配 */
    List<CampusRecruitment> findByTitleContainingOrderBySortOrderAscIdAsc(String keyword);

    /** 学生端列表：只返回已发布条目 */
    List<CampusRecruitment> findByStatusOrderBySortOrderAscIdAsc(String status);
}
