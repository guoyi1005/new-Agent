package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生生成的 Python 练习卷。
 *
 * 只记录「抽了哪些题」的快照，题目内容仍以 python_problem 为准，
 * 这样题库更新后试卷不会出现陈旧副本。
 */
@Data
@Entity
@Table(name = "python_paper", indexes = {
        @Index(name = "idx_python_paper_user", columnList = "user_id,created_at")
})
public class PythonPaper {

    /** 每题固定分值，与前端展示保持一致。 */
    public static final int SCORE_PER_QUESTION = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 160)
    private String title;

    /** all / easy / medium / hard */
    @Column(nullable = false, length = 20)
    private String difficulty = "all";

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(name = "question_ids_json", nullable = false, columnDefinition = "TEXT")
    private String questionIdsJson;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount = 0;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
