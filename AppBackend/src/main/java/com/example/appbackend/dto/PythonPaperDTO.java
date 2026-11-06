package com.example.appbackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Python 练习卷：保存与回看的传输对象。 */
public final class PythonPaperDTO {

    private PythonPaperDTO() {
    }

    /** 前端提交的保存请求。 */
    @Data
    public static class CreateRequest {
        @Size(max = 160, message = "试卷标题最多160字符")
        private String title;

        @Pattern(regexp = "all|easy|medium|hard", message = "试卷难度参数无效")
        private String difficulty;

        @Size(max = 20, message = "试卷标签最多20个")
        private List<@Size(max = 50, message = "单个标签最多50字符") String> tags = new ArrayList<>();

        @NotEmpty(message = "请先选择题目")
        @Size(max = 100, message = "单份试卷最多100题")
        private List<@NotNull(message = "题目ID不能为空") Long> questionIds = new ArrayList<>();
    }

    /** 返回给前端的试卷视图。 */
    @Data
    public static class PaperView {
        private Long id;
        private String title;
        private String difficulty;
        private List<String> tags = new ArrayList<>();
        private List<Long> questionIds = new ArrayList<>();
        private Integer questionCount;
        private Integer totalScore;
        private String createdAt;
    }
}
