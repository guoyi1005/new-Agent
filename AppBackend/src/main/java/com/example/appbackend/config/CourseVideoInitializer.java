package com.example.appbackend.config;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程视频：把外部公开课视频（B 站）挂到对应课程上。
 *
 * 只登记 BV 号与分P序号，播放时由前端使用 B 站官方外链播放器，
 * 视频内容始终在 B 站，本项目不下载、不转存、不代理。
 *
 * 只填充原本为空的字段，管理员在后台改过的不覆盖。
 * 注意：BV 号由人工核对后填入，无法自动校验有效性。
 */
@Component
@Order(230)
public class CourseVideoInitializer implements ApplicationRunner {

    private final CampusCourseRepository courseRepository;
    private final CampusCourseChapterRepository chapterRepository;

    public CourseVideoInitializer(CampusCourseRepository courseRepository,
                                  CampusCourseChapterRepository chapterRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 分P映射按「章节顺序 -> 视频分P」登记。只登记已确认的第 1 集，
        // 其余章节留空，避免把不确定的分P号写死导致播放器报错。
        List<CourseVideo> videos = List.of(
                new CourseVideo("测试基础", "BV1TP4y1J7BD", Map.of(1, 1)),
                new CourseVideo("Linux 与网络", "BV1yE421V7a2", Map.of(1, 1)),
                new CourseVideo("自动化测试", "BV1Y9UPYAEqN", Map.of(1, 1)),
                new CourseVideo("Python 编程基础", "BV1qW4y1a7fU", Map.of(1, 1)),
                new CourseVideo("MySQL 数据库基础", "BV1Kr4y1i7ru", Map.of(1, 1)),
                new CourseVideo("FastAPI 接口开发", "BV1LW3K63ExS", Map.of(1, 1)),
                new CourseVideo("Java 程序设计基础", "BV17F411T7Ao", Map.of(1, 1)),
                new CourseVideo("Spring Boot 后端开发", "BV1Lq4y1J77x", Map.of(1, 1)),
                new CourseVideo("前端开发基础", "BV1kM4y127Li", Map.of(1, 1)),
                new CourseVideo("Vue 3 组件开发", "BV1qxGA6KEta", Map.of(1, 1))
        );

        Map<String, CampusCourse> coursesByName = new LinkedHashMap<>();
        for (CampusCourse course : courseRepository.findAll()) {
            coursesByName.putIfAbsent(course.getName(), course);
        }
        for (CourseVideo video : videos) {
            CampusCourse course = coursesByName.get(video.courseName());
            if (course == null) continue;
            if (course.getVideoBvid() == null || course.getVideoBvid().isBlank()) {
                course.setVideoBvid(video.bvid());
                course = courseRepository.save(course);
            }
            for (CampusCourseChapter chapter
                    : chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())) {
                if (chapter.getVideoPage() != null) continue;
                Integer page = video.pageByChapterOrder().get(chapter.getSortOrder());
                if (page == null) continue;
                chapter.setVideoPage(page);
                chapterRepository.save(chapter);
            }
        }
    }

    private record CourseVideo(String courseName, String bvid, Map<Integer, Integer> pageByChapterOrder) { }
}