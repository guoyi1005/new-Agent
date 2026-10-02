package com.example.appbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 本地岗位列表项，只含招聘网站上公开展示的信息。 */
@Data
public class LocalJobPostingDTO {

    private Long id;
    private String jobTitle;
    private String company;
    private String city;
    private String district;
    private String salaryText;
    private String education;
    private String jobCategory;
    private String sourceName;
    private String sourceKeys;
    private String detailUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime lastSeenAt;
}
