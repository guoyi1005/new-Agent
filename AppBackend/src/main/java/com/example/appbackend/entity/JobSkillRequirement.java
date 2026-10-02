package com.example.appbackend.entity;
import jakarta.persistence.*; import lombok.Data; import java.time.LocalDateTime;
@Data @Entity @Table(name="job_skill_requirement", indexes={@Index(name="idx_job_skill_job",columnList="job_code"),@Index(name="idx_job_skill_skill",columnList="skill_id")})
public class JobSkillRequirement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="job_code",nullable=false,length=80) private String jobCode;
 @Column(name="job_name",nullable=false,length=120) private String jobName;
 @Column(name="skill_id",nullable=false) private Long skillId;
 @Column(name="required_level",nullable=false) private Integer requiredLevel;
 @Column(nullable=false) private Double importance=0.5;
 @Column(name="required_flag",nullable=false) private Boolean requiredFlag=true;
 @Column(name="sort_order",nullable=false) private Integer sortOrder=0;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @PrePersist void create(){createdAt=LocalDateTime.now(); if(importance==null)importance=.5; if(requiredFlag==null)requiredFlag=true; if(sortOrder==null)sortOrder=0;}
}