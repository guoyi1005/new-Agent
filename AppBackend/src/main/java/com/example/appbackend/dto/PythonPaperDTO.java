package com.example.appbackend.dto;

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
        private String title;
        private String difficulty;
        private List<String> tags = new ArrayList<>();
        private List<Long> questionIds = new ArrayList<>();
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
