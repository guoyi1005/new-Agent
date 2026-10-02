package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "business_sandbox_room", indexes = {
        @Index(name = "idx_bsr_host", columnList = "host_user_id"),
        @Index(name = "idx_bsr_code", columnList = "room_code", unique = true)
})
public class BusinessSandboxRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, unique = true, length = 12)
    private String roomCode;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "scenario_id", nullable = false, length = 80)
    private String scenarioId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 24)
    private String status = "WAITING";

    @Column(name = "current_round", nullable = false)
    private Integer currentRound = 1;

    @Column(name = "round_count", nullable = false)
    private Integer roundCount = 4;

    @Column(name = "max_companies", nullable = false)
    private Integer maxCompanies = 4;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 5;

    @Lob
    @Column(name = "state_json", nullable = false, columnDefinition = "LONGTEXT")
    private String stateJson;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}