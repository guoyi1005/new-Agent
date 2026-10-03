package com.example.appbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 职业路径图谱：岗位节点 + 岗位关系 + 每个关系的迁移分析。
 *
 * 星球的大小与坐标完全来自管理端配置，这里只补充匹配度、技能差距与推荐标签，
 * 前端不得用这些数值去改变星球尺寸或位置。
 */
@Data
@Schema(description = "职业路径图谱数据")
public class CareerPathDTO {

    @Schema(description = "星图里的岗位节点，附带人岗匹配结果")
    private List<JobNode> jobs = new ArrayList<>();

    @Schema(description = "管理端配置的岗位关系，附带迁移分析")
    private List<RelationView> relations = new ArrayList<>();

    @Schema(description = "当前学生的成长画像状态")
    private UserDataStatus userData = new UserDataStatus();

    @Data
    @Schema(description = "岗位节点")
    public static class JobNode {

        @Schema(description = "星图星球 id")
        private String jobId;

        @Schema(description = "岗位名称")
        private String name;

        @Schema(description = "技能体系里的岗位标识，未绑定时为空")
        private String jobCode;

        @Schema(description = "岗位方向")
        private String direction;

        @Schema(description = "岗位类型")
        private String type;

        @Schema(description = "是否有可用的匹配结果")
        private Boolean hasMatch = false;

        @Schema(description = "加权匹配度 0-100")
        private Integer matchRate;

        @Schema(description = "已具备的技能数")
        private Integer masteredCount;

        @Schema(description = "待补齐的技能数")
        private Integer missingCount;

        @Schema(description = "最需要补的技能")
        private String topGap;

        @Schema(description = "数据状态")
        private String dataStatus;
    }

    @Data
    @Schema(description = "岗位关系 + 迁移分析")
    public static class RelationView {

        private Long id;

        private String sourceJobId;
        private String sourceName;
        private String targetJobId;
        private String targetName;

        @Schema(description = "关系类型 PROMOTION / TRANSFER / RELATED / BRANCH")
        private String relationType;

        @Schema(description = "关系名称，例如「横向转岗」")
        private String relationName;

        private String description;

        @Schema(description = "连线类型 SOLID / DASHED / THIN / GLOW")
        private String lineType;

        @Schema(description = "推荐程度 0-100")
        private Integer recommendation;

        private Boolean enabled;
        private Integer sortOrder;

        /* ---------- 目标岗位的迁移分析 ---------- */

        @Schema(description = "目标岗位在技能体系里的标识")
        private String jobCode;

        private String direction;
        private String type;

        @Schema(description = "目标岗位是否有匹配结果")
        private Boolean hasMatch = false;

        @Schema(description = "技能匹配度 0-100")
        private Integer matchRate;

        @Schema(description = "岗位要求技能总数")
        private Integer totalSkills;

        @Schema(description = "已具备技能数")
        private Integer masteredCount;

        @Schema(description = "待补齐技能数")
        private Integer missingCount;

        @Schema(description = "可复用能力")
        private List<String> reusableSkills = new ArrayList<>();

        @Schema(description = "需要新增的能力")
        private List<String> missingSkills = new ArrayList<>();

        @Schema(description = "岗位要求的全部技能")
        private List<String> requiredSkills = new ArrayList<>();

        @Schema(description = "转向难度：低 / 中 / 高")
        private String difficulty;

        private String dataStatus;
        private String dataStatusText;
        private String advice;
    }

    @Data
    @Schema(description = "成长画像数据状态")
    public static class UserDataStatus {

        @Schema(description = "是否有可用记录")
        private Boolean ready = false;

        @Schema(description = "学习记录条数")
        private Integer evidenceCount = 0;

        @Schema(description = "insufficient / partial / ready")
        private String status;

        private String text;
    }
}
