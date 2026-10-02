package com.example.appbackend.entity;
import jakarta.persistence.*; import lombok.Data; import java.time.LocalDateTime;
@Data @Entity @Table(name="learning_skill", indexes=@Index(name="idx_learning_skill_code",columnList="skill_code",unique=true))
public class LearningSkill {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="skill_code",nullable=false,unique=true,length=80) private String code;
 @Column(nullable=false,length=120) private String name;
 @Column(length=80) private String category;
 @Column(length=500) private String description;
 @Column(name="sort_order",nullable=false) private Integer sortOrder=0;
 @Column(nullable=false,length=20) private String status="ACTIVE";
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @PrePersist void create(){createdAt=LocalDateTime.now(); if(status==null)status="ACTIVE"; if(sortOrder==null)sortOrder=0;}
}