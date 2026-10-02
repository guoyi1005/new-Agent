package com.example.appbackend.entity;
import jakarta.persistence.*; import lombok.Data; import java.time.LocalDateTime;
@Data @Entity @Table(name="learning_content_skill", indexes={@Index(name="idx_lcs_content",columnList="source_type,source_id"),@Index(name="idx_lcs_skill",columnList="skill_id")})
public class LearningContentSkill {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="source_type",nullable=false,length=30) private String sourceType;
 @Column(name="source_id",nullable=false) private Long sourceId;
 @Column(name="skill_id",nullable=false) private Long skillId;
 @Column(nullable=false) private Double relevance=.5;
 @Column(name="target_level",nullable=false) private Integer targetLevel=60;
 @Column(name="primary_skill",nullable=false) private Boolean primarySkill=false;
 @Column(name="recommendation_order",nullable=false) private Integer recommendationOrder=0;
 @Column(length=20) private String difficulty;
 @Column(name="prerequisite_skill_id") private Long prerequisiteSkillId;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @PrePersist void create(){createdAt=LocalDateTime.now(); if(relevance==null)relevance=.5; if(targetLevel==null)targetLevel=60; if(primarySkill==null)primarySkill=false; if(recommendationOrder==null)recommendationOrder=0;}
}