package com.example.appbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 人岗匹配结果：岗位要求技能与学生当前技能水平的对照。
 * 匹配度由「技能达成率 × 岗位重要性」加权得出，明细一起返回，保证分数可解释。
 */
@Data
@Schema(description = "人岗匹配结果")
public class CareerFitDTO {

    @Schema(description = "岗位标识", example = "python-backend")
    private String jobCode;

    @Schema(description = "岗位名称", example = "Python 开发工程师")
    private String jobName;

    @Schema(description = "岗位方向", example = "后端开发 / 服务端")
    private String direction;

    @Schema(description = "岗位类型", example = "应届 / 实习")
    private String type;

    @Schema(description = "岗位说明")
    private String summary;

    @Schema(description = "岗位职责")
    private List<String> responsibilities = new ArrayList<>();

    @Schema(description = "提升建议")
    private String advice;

    @Schema(description = "加权匹配度 0-100", example = "52")
    private Integer matchRate;

    @Schema(description = "参与计算的技能数", example = "7")
    private Integer totalSkills;

    @Schema(description = "已有学习记录覆盖的技能数", example = "2")
    private Integer coveredSkills;

    @Schema(description = "已掌握的技能")
    private List<SkillFit> mastered = new ArrayList<>();

    @Schema(description = "待提升的技能，按重要性与差距排序")
    private List<SkillFit> toImprove = new ArrayList<>();

    @Schema(description = "全部技能对照明细")
    private List<SkillFit> gaps = new ArrayList<>();

    @Schema(description = "数据状态: insufficient-证据不足, partial-部分证据, ready-可用")
    private String dataStatus;

    @Schema(description = "数据状态说明")
    private String dataStatusText;

    @Schema(description = "学习记录条数", example = "12")
    private Integer evidenceCount;

    @Schema(description = "技能等级来源说明")
    private String levelSourceText;

    @Data
    @Schema(description = "单个技能对照")
    public static class SkillFit {

        @Schema(description = "技能标识", example = "python-basic")
        private String skillCode;

        @Schema(description = "技能名称", example = "Python 基础")
        private String skillName;

        @Schema(description = "当前水平 0-100", example = "40")
        private Integer current;

        @Schema(description = "岗位要求 0-100", example = "90")
        private Integer required;

        @Schema(description = "差距 = 要求 - 当前", example = "50")
        private Integer gap;

        @Schema(description = "岗位重要性 0-1", example = "1.0")
        private Double importance;

        @Schema(description = "达成率 0-1", example = "0.44")
        private Double ratio;
    }

    @Data
    @Schema(description = "岗位匹配排行项")
    public static class JobFitSummary {

        @Schema(description = "岗位标识")
        private String jobCode;

        @Schema(description = "岗位名称")
        private String jobName;

        @Schema(description = "岗位方向")
        private String direction;

        @Schema(description = "岗位类型")
        private String type;

        @Schema(description = "加权匹配度 0-100")
        private Integer matchRate;

        @Schema(description = "已掌握技能数")
        private Integer masteredCount;

        @Schema(description = "最明显的优势技能")
        private String topStrength;

        @Schema(description = "最需要补的技能")
        private String topGap;

        @Schema(description = "岗位要求的主要技能，按重要性排序")
        private List<String> topSkills = new ArrayList<>();

        @Schema(description = "数据状态")
        private String dataStatus;
    }
}
