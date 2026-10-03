package com.example.appbackend.config;

import com.example.appbackend.service.LearningDemoDataSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 仅在显式启用演示开关时补齐「我的练习」演示学习数据。
 *
 * 排在 LearningTaxonomyInitializer（技能字典、课程、课程技能关联）之后执行，
 * 保证生成数据时依赖的课程与技能已经存在。
 */
@Component
@Order(250)
public class LearningDemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LearningDemoDataInitializer.class);

    private final LearningDemoDataSeeder seeder;

    public LearningDemoDataInitializer(LearningDemoDataSeeder seeder) {
        this.seeder = seeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seeder.enabled()) {
            log.info("演示学习数据已关闭（app.demo-learning-data.enabled=false）");
            return;
        }
        int created = seeder.seedAllStudents();
        if (created > 0) {
            log.info("已为 {} 个学生账号补齐「我的练习」演示学习数据", created);
        }
    }
}
