package com.example.appbackend.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Python 知识点 key → 中文展示名。
 *
 * 考试与题库侧落库时，知识点名称有时直接等于 key（例如 python.expression.arithmetic），
 * 直接把 key 渲染到页面上非常难读。这里维护一份权威展示名，
 * 供学习路径、知识图谱与掌握度视图统一取用，避免同一知识点在不同页面显示不一致。
 */
public final class KnowledgePointNames {

    private static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();

    static {
        DISPLAY_NAMES.put("python.expression.arithmetic", "算术表达式");
        DISPLAY_NAMES.put("python.data_type.collection", "集合类型");
        DISPLAY_NAMES.put("python.data_type.sequence", "序列类型");
        DISPLAY_NAMES.put("python.function.syntax", "函数定义");
        DISPLAY_NAMES.put("python.exception.application", "异常处理实践");
        DISPLAY_NAMES.put("python.exception.reliability", "异常与可靠性");
        DISPLAY_NAMES.put("python.algorithm.complexity", "算法复杂度");
    }

    private KnowledgePointNames() {
    }

    /**
     * 传入知识点 key 或名称，返回可直接展示的文本。
     * 已登记的知识点返回中文名；句子中夹带 key 的也会被替换；
     * 没有映射时原样返回，不做猜测。
     */
    public static String display(String keyOrName) {
        if (keyOrName == null) return null;
        String text = keyOrName.trim();
        if (text.isEmpty()) return text;
        String mapped = DISPLAY_NAMES.get(text);
        if (mapped != null) return mapped;
        String result = text;
        for (Map.Entry<String, String> entry : DISPLAY_NAMES.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
