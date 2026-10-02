package com.example.appbackend.entity;
import jakarta.persistence.*; import lombok.Data; import java.time.LocalDateTime;
@Data @Entity @Table(name="learning_record", indexes={@Index(name="idx_learning_record_user",columnList="user_id,occurred_at"),@Index(name="idx_learning_record_skill",columnList="user_id,skill_id"),@Index(name="idx_learning_record_event",columnList="event_id",unique=true)})
public class LearningRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="skill_id") private Long skillId;
 @Column(name="source_type",nullable=false,length=30) private String sourceType;
 @Column(name="source_id",nullable=false) private Long sourceId;
 @Column(name="action_type",nullable=false,length=40) private String actionType;
 @Column(nullable=false) private Integer progress=0;
 @Column private Double score;
 @Column(length=500) private String evidence;
 @Column(name="metadata_json",columnDefinition="LONGTEXT") private String metadataJson;
 @Column(name="event_id",nullable=false,length=180,unique=true) private String eventId;
 @Column(name="occurred_at",nullable=false) private LocalDateTime occurredAt;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @PrePersist void create(){createdAt=LocalDateTime.now(); if(occurredAt==null)occurredAt=createdAt; if(progress==null)progress=0;}
}