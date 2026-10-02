package com.example.appbackend.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 把「题目标签」和「学习路径节点文本」映射到统一技能编码，
 * 让算法题、项目实训与课程使用同一套技能字典。
 *
 * 题目标签是一对一精确映射（题库自己已有的标签），不做猜测；
 * 路径节点是自由文本，使用关键词表匹配，最多取 3 个技能。
 */
public final class SkillTextMatcher {

    /** 数据结构与算法总技能：任何算法题都会同时计入该技能。 */
    public static final String DATA_STRUCTURES = "data-structures";

    private static final int MAX_TEXT_SKILLS = 3;

    /** 题库标签 -> 技能编码（精确匹配题库现有 26 个标签）。 */
    private static final Map<String, String> PROBLEM_TAG_SKILLS = new LinkedHashMap<>();

    /** 关键词 -> 技能编码，键统一小写，用于路径节点等自由文本。 */
    private static final Map<String, String> TEXT_KEYWORDS = new LinkedHashMap<>();

    static {
        PROBLEM_TAG_SKILLS.put("数组", "algo-array-hash");
        PROBLEM_TAG_SKILLS.put("哈希表", "algo-array-hash");
        PROBLEM_TAG_SKILLS.put("前缀和", "algo-array-hash");
        PROBLEM_TAG_SKILLS.put("滑动窗口", "algo-array-hash");
        PROBLEM_TAG_SKILLS.put("双指针", "algo-array-hash");
        PROBLEM_TAG_SKILLS.put("位运算", "algo-array-hash");
        PROBLEM_TAG_SKILLS.put("链表", "algo-linked-list");
        PROBLEM_TAG_SKILLS.put("栈", "algo-linked-list");
        PROBLEM_TAG_SKILLS.put("队列", "algo-linked-list");
        PROBLEM_TAG_SKILLS.put("单调栈", "algo-linked-list");
        PROBLEM_TAG_SKILLS.put("单调队列", "algo-linked-list");
        PROBLEM_TAG_SKILLS.put("堆", "algo-linked-list");
        PROBLEM_TAG_SKILLS.put("字符串", "algo-string");
        PROBLEM_TAG_SKILLS.put("递归", "algo-recursion-dp");
        PROBLEM_TAG_SKILLS.put("迭代", "algo-recursion-dp");
        PROBLEM_TAG_SKILLS.put("动态规划", "algo-recursion-dp");
        PROBLEM_TAG_SKILLS.put("分治", "algo-recursion-dp");
        PROBLEM_TAG_SKILLS.put("回溯", "algo-recursion-dp");
        PROBLEM_TAG_SKILLS.put("排序", "algo-search-sort");
        PROBLEM_TAG_SKILLS.put("二分查找", "algo-search-sort");
        PROBLEM_TAG_SKILLS.put("广度优先搜索", "algo-graph-tree");
        PROBLEM_TAG_SKILLS.put("深度优先搜索", "algo-graph-tree");
        PROBLEM_TAG_SKILLS.put("并查集", "algo-graph-tree");
        PROBLEM_TAG_SKILLS.put("贪心", "algo-greedy-math");
        PROBLEM_TAG_SKILLS.put("数学", "algo-greedy-math");
        PROBLEM_TAG_SKILLS.put("设计", "algo-design");

        TEXT_KEYWORDS.put("vue", "vue3");
        TEXT_KEYWORDS.put("typescript", "typescript");
        TEXT_KEYWORDS.put("javascript", "javascript");
        TEXT_KEYWORDS.put("html", "html-css");
        TEXT_KEYWORDS.put("css", "html-css");
        TEXT_KEYWORDS.put("页面布局", "html-css");
        TEXT_KEYWORDS.put("前端工程", "frontend-engineering");
        TEXT_KEYWORDS.put("组件", "vue3");
        TEXT_KEYWORDS.put("spring boot", "spring-boot");
        TEXT_KEYWORDS.put("springboot", "spring-boot");
        TEXT_KEYWORDS.put("java 基础", "java-basic");
        TEXT_KEYWORDS.put("java", "java-basic");
        TEXT_KEYWORDS.put("redis", "redis");
        TEXT_KEYWORDS.put("缓存", "redis");
        TEXT_KEYWORDS.put("大模型", "llm-app");
        TEXT_KEYWORDS.put("llm", "llm-app");
        TEXT_KEYWORDS.put("提示词", "llm-app");
        TEXT_KEYWORDS.put("向量", "vector-db");
        TEXT_KEYWORDS.put("检索", "vector-db");
        TEXT_KEYWORDS.put("pytorch", "pytorch");
        TEXT_KEYWORDS.put("深度学习", "pytorch");
        TEXT_KEYWORDS.put("神经网络", "pytorch");
        TEXT_KEYWORDS.put("数学", "math-stat");
        TEXT_KEYWORDS.put("统计", "statistics");
        TEXT_KEYWORDS.put("可视化", "statistics");
        TEXT_KEYWORDS.put("数据分析", "data-analysis");
        TEXT_KEYWORDS.put("数据处理", "data-processing");
        TEXT_KEYWORDS.put("需求分析", "product-planning");
        TEXT_KEYWORDS.put("产品规划", "product-planning");
        TEXT_KEYWORDS.put("用户研究", "user-research");
        TEXT_KEYWORDS.put("原型", "prototyping");
        TEXT_KEYWORDS.put("交互设计", "prototyping");
        TEXT_KEYWORDS.put("数据类型", "python-basic");
        TEXT_KEYWORDS.put("流程控制", "python-basic");
        TEXT_KEYWORDS.put("基础语法", "python-basic");
        TEXT_KEYWORDS.put("表设计", "mysql");
        TEXT_KEYWORDS.put("查询", "mysql");
        TEXT_KEYWORDS.put("fastapi", "fastapi");
        TEXT_KEYWORDS.put("rest", "fastapi");
        TEXT_KEYWORDS.put("接口", "fastapi");
        TEXT_KEYWORDS.put("api", "fastapi");
        TEXT_KEYWORDS.put("路由", "fastapi");
        TEXT_KEYWORDS.put("mysql", "mysql");
        TEXT_KEYWORDS.put("数据库", "mysql");
        TEXT_KEYWORDS.put("sql", "mysql");
        TEXT_KEYWORDS.put("索引", "mysql");
        TEXT_KEYWORDS.put("事务", "mysql");
        TEXT_KEYWORDS.put("面向对象", "python-advanced");
        TEXT_KEYWORDS.put("装饰器", "python-advanced");
        TEXT_KEYWORDS.put("生成器", "python-advanced");
        TEXT_KEYWORDS.put("异步", "python-advanced");
        TEXT_KEYWORDS.put("并发", "python-advanced");
        TEXT_KEYWORDS.put("python", "python-basic");
        TEXT_KEYWORDS.put("语法", "python-basic");
        TEXT_KEYWORDS.put("函数", "python-basic");
        TEXT_KEYWORDS.put("字典", "python-basic");
        TEXT_KEYWORDS.put("列表", "python-basic");
        TEXT_KEYWORDS.put("git", "git");
        TEXT_KEYWORDS.put("版本控制", "git");
        TEXT_KEYWORDS.put("linux", "linux");
        TEXT_KEYWORDS.put("shell", "linux");
        TEXT_KEYWORDS.put("部署", "linux");
        TEXT_KEYWORDS.put("http", "networking");
        TEXT_KEYWORDS.put("网络", "networking");
        TEXT_KEYWORDS.put("协议", "networking");
        TEXT_KEYWORDS.put("自动化", "automation-testing");
        TEXT_KEYWORDS.put("selenium", "automation-testing");
        TEXT_KEYWORDS.put("pytest", "automation-testing");
        TEXT_KEYWORDS.put("性能", "performance-testing");
        TEXT_KEYWORDS.put("压测", "performance-testing");
        TEXT_KEYWORDS.put("用例设计", "test-case-design");
        TEXT_KEYWORDS.put("等价类", "test-case-design");
        TEXT_KEYWORDS.put("边界值", "test-case-design");
        TEXT_KEYWORDS.put("测试", "software-testing");
        TEXT_KEYWORDS.put("算法", DATA_STRUCTURES);
        TEXT_KEYWORDS.put("数据结构", DATA_STRUCTURES);
        TEXT_KEYWORDS.put("项目", "python-basic");
    }

    private SkillTextMatcher() {
    }

    /**
     * 题目标签 -> 技能编码。命中任一算法标签时会额外加入 data-structures 总技能，
     * 让「刷题」能够拉动岗位要求的“数据结构与算法”进度。
     */
    public static List<String> skillsForProblemTags(List<String> tags) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                if (tag == null || tag.isBlank()) continue;
                String code = PROBLEM_TAG_SKILLS.get(tag.trim());
                if (code != null) codes.add(code);
            }
        }
        if (!codes.isEmpty()) codes.add(DATA_STRUCTURES);
        return new ArrayList<>(codes);
    }

    /** 自由文本 -> 技能编码，最多 {@value #MAX_TEXT_SKILLS} 个。 */
    public static List<String> matchText(String text) {
        if (text == null || text.isBlank()) return List.of();
        String lower = text.toLowerCase();
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : TEXT_KEYWORDS.entrySet()) {
            if (!lower.contains(entry.getKey())) continue;
            codes.add(entry.getValue());
            if (codes.size() >= MAX_TEXT_SKILLS) break;
        }
        return new ArrayList<>(codes);
    }
}