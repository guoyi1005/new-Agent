package com.example.appbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 校园招聘汇总：学生端「校园招聘」板块展示用的统计与日程。
 */
@Data
@Schema(description = "校园招聘汇总")
public class CampusRecruitmentSummaryDTO {

    @Schema(description = "招聘季", example = "2027 届秋招")
    private String season;

    @Schema(description = "宣讲会场次", example = "12")
    private Integer talks = 0;

    @Schema(description = "双选会场次", example = "3")
    private Integer fairs = 0;

    @Schema(description = "校招岗位数", example = "42")
    private Integer roles = 0;

    @Schema(description = "近期日程")
    private List<ScheduleItem> schedule = new ArrayList<>();

    @Data
    @Schema(description = "校招日程条目")
    public static class ScheduleItem {

        @Schema(description = "日期", example = "2026-10-08")
        private String date;

        @Schema(description = "事项", example = "企业宣讲")
        private String title;

        public ScheduleItem() {
        }

        public ScheduleItem(String date, String title) {
            this.date = date;
            this.title = title;
        }
    }
}
