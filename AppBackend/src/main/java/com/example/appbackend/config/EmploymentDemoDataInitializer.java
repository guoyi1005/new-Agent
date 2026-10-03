package com.example.appbackend.config;

import com.example.appbackend.entity.AlumniEnterprise;
import com.example.appbackend.entity.CampusRecruitment;
import com.example.appbackend.repository.AlumniEnterpriseRepository;
import com.example.appbackend.repository.CampusRecruitmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 就业服务初始内容。
 *
 * 说明：
 * 1. 只在对应表完全为空时写入，管理端一旦维护过内容就不再介入；
 * 2. 校友企业沿用实习就业页已经展示的企业名单，没有新增杜撰的企业；
 * 3. 校园招聘沿用页面已有的宣讲会、双选会与专场招聘日程。
 */
@Component
@Order(250)
public class EmploymentDemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmploymentDemoDataInitializer.class);

    private final AlumniEnterpriseRepository alumniRepository;
    private final CampusRecruitmentRepository campusRepository;

    public EmploymentDemoDataInitializer(AlumniEnterpriseRepository alumniRepository,
                                         CampusRecruitmentRepository campusRepository) {
        this.alumniRepository = alumniRepository;
        this.campusRepository = campusRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAlumni();
        seedCampusRecruitments();
    }

    private void seedAlumni() {
        if (alumniRepository.count() > 0) {
            return;
        }
        alumniRepository.saveAll(List.of(
                alumni("成都风雨兴科技有限公司", "风雨", "软件与信息服务", "Python,Java", 3, true, 2, 1),
                alumni("成都巡洋船舶管理有限公司", "巡洋", "航运与物流", "Java", 2, true, 1, 2),
                alumni("成都屿西半导体科技有限公司", "屿西", "半导体与电子", "AI,Python", 1, false, 0, 3)
        ));
        log.info("已写入校友企业初始内容 {} 条", alumniRepository.count());
    }

    private AlumniEnterprise alumni(String name, String shortName, String industry, String fields,
                                    int alumniCount, boolean hiring, int openPositions, int sortOrder) {
        AlumniEnterprise entity = new AlumniEnterprise();
        entity.setName(name);
        entity.setShortName(shortName);
        entity.setIndustry(industry);
        entity.setFields(fields);
        entity.setAlumniCount(alumniCount);
        entity.setHiring(hiring);
        entity.setOpenPositions(openPositions);
        entity.setSortOrder(sortOrder);
        entity.setEnabled(true);
        return entity;
    }

    private void seedCampusRecruitments() {
        if (campusRepository.count() > 0) {
            return;
        }
        campusRepository.saveAll(List.of(
                recruitment("企业宣讲", "成都风雨兴科技有限公司", CampusRecruitment.TYPE_TALK,
                        "大学生活动中心", LocalDate.of(2026, 10, 8), 12, 1),
                recruitment("秋季双选会", "成都巡洋船舶管理有限公司", CampusRecruitment.TYPE_FAIR,
                        "校体育馆", LocalDate.of(2026, 10, 12), 18, 2),
                recruitment("专场招聘", "成都屿西半导体科技有限公司", CampusRecruitment.TYPE_JOB,
                        "就业指导中心", LocalDate.of(2026, 10, 18), 12, 3)
        ));
        log.info("已写入校园招聘初始内容 {} 条", campusRepository.count());
    }

    private CampusRecruitment recruitment(String title, String company, String type, String location,
                                          LocalDate eventDate, int roleCount, int sortOrder) {
        CampusRecruitment entity = new CampusRecruitment();
        entity.setTitle(title);
        entity.setCompany(company);
        entity.setType(type);
        entity.setSeason("2027 届秋招");
        entity.setCity("成都");
        entity.setLocation(location);
        entity.setEventDate(eventDate);
        entity.setRoleCount(roleCount);
        entity.setStatus(CampusRecruitment.STATUS_PUBLISHED);
        entity.setSortOrder(sortOrder);
        return entity;
    }
}
