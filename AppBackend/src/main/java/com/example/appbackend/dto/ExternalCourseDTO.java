package com.example.appbackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 外部精选课程视图：含平台、官方链接与技能/岗位标签。 */
@Data
public class ExternalCourseDTO {
    private Long id;
    private String title;
    private String provider;
    private String url;
    private String description;
    private String level;
    private Boolean free;
    private List<String> skills = new ArrayList<>();
    private List<String> jobs = new ArrayList<>();
}