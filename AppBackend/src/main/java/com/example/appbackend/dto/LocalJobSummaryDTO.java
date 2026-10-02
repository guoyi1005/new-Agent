package com.example.appbackend.dto;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** 「成都本地就业」板块的统计：来源是本地岗位表，抓取时间取自最近一次收录时间。 */
@Data
public class LocalJobSummaryDTO {

    private long total;
    private long todayNew;
    private long intern;
    private long campus;
    private long stateOwned;
    private LocalDateTime updatedAt;
    private List<CountItem> districts;
    private List<CountItem> sources;
    /** 收录过这些岗位的全部平台 key（rc114 / cd-sc91 / sc91）。 */
    private List<String> platforms;

    @Getter
    public static class CountItem {
        private final String name;
        private final long total;

        public CountItem(String name, long total) {
            this.name = name;
            this.total = total;
        }
    }
}
